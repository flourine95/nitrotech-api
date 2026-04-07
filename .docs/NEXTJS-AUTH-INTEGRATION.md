# Tích hợp Auth với Next.js 16 — BFF Pattern

Backend: Spring Boot + JWT + HttpOnly Cookie  
Frontend: Next.js 16 (App Router), BFF qua Route Handlers + Server Actions

---

## Kiến trúc tổng quan

```
Browser → Next.js BFF (Route Handler) → Spring Boot
```

- Browser chỉ gọi API cùng domain Next.js
- Next.js giữ session server-side (encrypted), browser không thấy token
- Spring giữ refreshToken trong HttpOnly cookie
- **Cookie từ Spring không tự về browser** khi gọi server-to-server — Next.js phải đọc `Set-Cookie` từ Spring rồi set lại cho browser

### Phân loại endpoint

| Nhóm | Ví dụ | Cách gọi |
|---|---|---|
| Auth | login, logout, refresh, me | Route Handler Next.js → Spring |
| Private | orders, cart, profile, address, wishlist, review | Route Handler Next.js → Spring |
| Public | products, categories, brands, banners | Gọi thẳng Spring (không cần token) |

---

## Backend API Reference

### POST /api/auth/login

Request:
```json
{ "email": "user@example.com", "password": "password123" }
```

Response 200:
```json
{
  "data": {
    "accessToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": { "id": 1, "name": "Nguyen Van A", "email": "user@example.com" }
  }
}
```
Response headers:
```
Set-Cookie: refreshToken=<uuid>; Path=/api/auth; Max-Age=2592000; HttpOnly; SameSite=Lax
```

---

### POST /api/auth/refresh

Request: không cần body — Next.js forward Cookie header từ browser  
Response 200:
```json
{
  "data": { "accessToken": "eyJhbGci...", "tokenType": "Bearer", "expiresIn": 900 }
}
```
Response headers: `Set-Cookie: refreshToken=<uuid-mới>; ...` (token mới sau rotation)  
Response 401: token sai, hết hạn, hoặc đã dùng

---

### POST /api/auth/logout

Response 200: `{ "data": null, "message": "Logged out successfully" }`  
Response headers: `Set-Cookie: refreshToken=; Max-Age=0; ...` (clear cookie)

---

## Cài đặt

### Dependencies

```bash
npm install iron-session
```

iron-session dùng để ký và mã hóa session cookie — không dùng base64 thuần.

---

### Biến môi trường

```env
# .env.local
BACKEND_URL=http://localhost:8080
SESSION_SECRET=your-32-char-minimum-secret-here
```

`BACKEND_URL` không có `NEXT_PUBLIC_` — chỉ dùng server-side.

---

## Cấu trúc file

```
app/
  (auth)/
    login/page.tsx
  dashboard/page.tsx
  api/
    auth/
      login/route.ts
      logout/route.ts
      refresh/route.ts
    me/route.ts
    orders/route.ts
lib/
  auth/
    session.ts       ← iron-session: đọc/ghi session
    cookie.ts        ← forward Set-Cookie từ Spring về browser
    guards.ts        ← getValidAccessToken()
proxy.ts
```

---

## Implementation

### lib/auth/session.ts

```ts
import { getIronSession, IronSession } from 'iron-session'
import { cookies } from 'next/headers'

export interface SessionData {
  accessToken: string
  expiresAt: number  // ms
  user: { id: number; name: string; email: string }
}

const sessionOptions = {
  cookieName: 'session',
  password: process.env.SESSION_SECRET!,
  cookieOptions: {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax' as const,
    path: '/',
    maxAge: 30 * 24 * 60 * 60,
  },
}

export async function getSession() {
  const cookieStore = await cookies()
  return getIronSession<SessionData>(cookieStore, sessionOptions)
}
```

---

### lib/auth/cookie.ts

Forward `Set-Cookie` từ Spring response về browser — đây là điểm quan trọng nhất.

```ts
import { cookies } from 'next/headers'

/**
 * Đọc Set-Cookie header từ Spring response và set lại cho browser qua Next.js.
 * Cần thiết vì server-to-server fetch không tự forward cookie về browser.
 */
export async function forwardSetCookie(springResponse: Response): Promise<void> {
  const setCookieHeader = springResponse.headers.getSetCookie?.() 
    ?? springResponse.headers.get('set-cookie')?.split(', ') 
    ?? []

  if (!setCookieHeader.length) return

  const cookieStore = await cookies()

  for (const cookieStr of setCookieHeader) {
    const parts = cookieStr.split(';').map(s => s.trim())
    const [nameValue, ...attrs] = parts
    const eqIdx = nameValue.indexOf('=')
    const name = nameValue.substring(0, eqIdx)
    const value = nameValue.substring(eqIdx + 1)

    const attrMap: Record<string, string | boolean> = {}
    for (const attr of attrs) {
      const [k, v] = attr.split('=')
      attrMap[k.toLowerCase()] = v ?? true
    }

    cookieStore.set(name, value, {
      httpOnly: attrMap['httponly'] === true,
      secure: attrMap['secure'] === true,
      sameSite: (attrMap['samesite'] as any) ?? 'lax',
      path: (attrMap['path'] as string) ?? '/',
      maxAge: attrMap['max-age'] ? Number(attrMap['max-age']) : undefined,
    })
  }
}
```

---

### lib/auth/guards.ts

```ts
import { getSession } from './session'
import { forwardSetCookie } from './cookie'

const BACKEND = process.env.BACKEND_URL

// Lock per-request để tránh nhiều refresh đồng thời trong cùng 1 request
// Lưu ý: không giải quyết race condition trên nhiều serverless instance
// Nếu cần mạnh hơn, dùng Redis lock
let refreshing: Promise<string | null> | null = null

export async function getValidAccessToken(
  requestCookieHeader?: string
): Promise<string | null> {
  const session = await getSession()
  if (!session.accessToken) return null

  // Còn hạn (buffer 30s)
  if (Date.now() < session.expiresAt - 30_000) return session.accessToken

  if (refreshing) return refreshing

  refreshing = (async () => {
    try {
      const res = await fetch(`${BACKEND}/api/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          // Forward refreshToken cookie từ browser lên Spring
          ...(requestCookieHeader ? { Cookie: requestCookieHeader } : {}),
        },
      })

      if (!res.ok) {
        await session.destroy()
        return null
      }

      // Forward Set-Cookie mới từ Spring về browser (rotation)
      await forwardSetCookie(res)

      const { data } = await res.json()
      session.accessToken = data.accessToken
      session.expiresAt = Date.now() + data.expiresIn * 1000
      await session.save()

      return data.accessToken
    } finally {
      refreshing = null
    }
  })()

  return refreshing
}
```

---

### Route Handler: login

```ts
// app/api/auth/login/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getSession } from '@/lib/auth/session'
import { forwardSetCookie } from '@/lib/auth/cookie'

const BACKEND = process.env.BACKEND_URL

export async function POST(request: NextRequest) {
  const body = await request.json()

  const springRes = await fetch(`${BACKEND}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })

  if (!springRes.ok) {
    const error = await springRes.json()
    return NextResponse.json(error, { status: springRes.status })
  }

  const { data } = await springRes.json()

  // Forward refreshToken cookie từ Spring về browser
  await forwardSetCookie(springRes)

  // Lưu session Next.js
  const session = await getSession()
  session.accessToken = data.accessToken
  session.expiresAt = Date.now() + data.expiresIn * 1000
  session.user = data.user
  await session.save()

  // Không trả accessToken về client — client chỉ cần user info
  return NextResponse.json({ user: data.user })
}
```

---

### Route Handler: refresh

```ts
// app/api/auth/refresh/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getValidAccessToken } from '@/lib/auth/guards'

export async function POST(request: NextRequest) {
  // Forward cookie header từ browser để Spring nhận refreshToken
  const cookieHeader = request.headers.get('cookie') ?? ''
  const token = await getValidAccessToken(cookieHeader)

  if (!token) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  return NextResponse.json({ ok: true })
}
```

---

### Route Handler: logout

```ts
// app/api/auth/logout/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getSession } from '@/lib/auth/session'
import { forwardSetCookie } from '@/lib/auth/cookie'

const BACKEND = process.env.BACKEND_URL

export async function POST(request: NextRequest) {
  const session = await getSession()
  const cookieHeader = request.headers.get('cookie') ?? ''

  if (session.accessToken) {
    const springRes = await fetch(`${BACKEND}/api/auth/logout`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${session.accessToken}`,
        Cookie: cookieHeader,
      },
    })
    // Forward clear-cookie từ Spring về browser
    await forwardSetCookie(springRes)
  }

  await session.destroy()
  return NextResponse.json({ ok: true })
}
```

---

### Route Handler mẫu — private API

```ts
// app/api/me/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { getValidAccessToken } from '@/lib/auth/guards'

const BACKEND = process.env.BACKEND_URL

export async function GET(request: NextRequest) {
  const cookieHeader = request.headers.get('cookie') ?? ''
  const token = await getValidAccessToken(cookieHeader)
  if (!token) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const res = await fetch(`${BACKEND}/api/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
  })

  return NextResponse.json(await res.json(), { status: res.status })
}
```

---

### Server Component

```ts
// app/dashboard/page.tsx
import { getSession } from '@/lib/auth/session'
import { redirect } from 'next/navigation'

export default async function DashboardPage() {
  const session = await getSession()
  if (!session.user) redirect('/login')

  // Chỉ đọc session — không set cookie ở đây
  return <div>Xin chào, {session.user.name}</div>
}
```

---

### Login form (Client Component)

```tsx
// app/(auth)/login/page.tsx
'use client'

import { useRouter } from 'next/navigation'
import { useState } from 'react'

export default function LoginPage() {
  const router = useRouter()
  const [error, setError] = useState('')

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = new FormData(e.currentTarget)

    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: form.get('email'),
        password: form.get('password'),
      }),
    })

    if (!res.ok) {
      const err = await res.json()
      setError(err?.message || 'Đăng nhập thất bại')
      return
    }

    router.push('/dashboard')
  }

  return (
    <form onSubmit={handleSubmit}>
      <input name="email" type="email" placeholder="Email" required />
      <input name="password" type="password" placeholder="Mật khẩu" required />
      {error && <p>{error}</p>}
      <button type="submit">Đăng nhập</button>
    </form>
  )
}
```

---

### proxy.ts

```ts
// proxy.ts
import { NextRequest, NextResponse } from 'next/server'

const PROTECTED = ['/dashboard', '/profile', '/orders']

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl
  const isProtected = PROTECTED.some(p => pathname.startsWith(p))

  if (!isProtected) return NextResponse.next()

  // Chỉ check có session không — refresh xảy ra ở Route Handler
  if (!request.cookies.has('session')) {
    return NextResponse.redirect(new URL('/login', request.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/dashboard/:path*', '/profile/:path*', '/orders/:path*'],
}
```

---

## Những điều cần tránh

- Không gọi Spring trực tiếp từ Client Component cho endpoint cần auth
- Không set cookie trong Server Component
- Không dùng base64 để "mã hóa" session — dùng iron-session
- Không bỏ qua `forwardSetCookie` sau login/refresh/logout — nếu thiếu, rotation sẽ hỏng
- Không để `credentials: 'include'` trong server-side fetch — không có tác dụng, phải tự forward `Cookie` header

---

## Lưu ý về race condition

Lock `refreshing` trong `guards.ts` chỉ hiệu quả trong 1 Node.js instance. Với môi trường serverless (Vercel) hoặc nhiều instance, vẫn có thể xảy ra race condition. Giải pháp triệt để là dùng Redis lock hoặc chấp nhận rằng rotation sẽ tự xử lý (token cũ bị revoke, request thứ 2 sẽ fail và user cần login lại).
