# Documentation

This directory contains comprehensive documentation for the Nitrotech API project.

## Files

### ARCHITECTURE.md
System architecture and design patterns.

Contents:
- Layered architecture overview
- Project structure and data flow
- Layer responsibilities with code examples
- Key patterns (Soft Delete, Pagination, Validation, Exception Hierarchy)
- Module checklist
- Technology stack

Target audience: Developers, architects

### CODING-STANDARDS.md
Coding conventions and best practices.

Contents:
- Core rules (imports, type safety, streams, logging)
- Java standards (Lombok, dependency injection, null safety)
- Database and JPA standards (entity design, soft delete, transactions, batch queries)
- API conventions (URL design, validation, error codes, controller patterns)
- Exception handling with GlobalExceptionHandler
- Testing strategy
- Naming conventions
- Validation checklist

Target audience: All developers

### DATABASE-DESIGN.md
Database schema and design principles.

Contents:
- Design principles
- Soft delete pattern
- Complete schema for all 18 tables
- Relationships and foreign keys
- Migration order

Target audience: Backend developers, database administrators

## Quick Reference

### For New Developers
1. Read [ARCHITECTURE.md](./ARCHITECTURE.md) - Understand system design
2. Study [CODING-STANDARDS.md](./CODING-STANDARDS.md) - Learn coding conventions
3. Reference [DATABASE-DESIGN.md](./DATABASE-DESIGN.md) - Understand data model

### For Frontend Developers
1. Swagger UI at `http://localhost:8080/swagger-ui.html` - Interactive API documentation
2. [DATABASE-DESIGN.md](./DATABASE-DESIGN.md) - Data structure and relationships

### For DevOps
1. [ARCHITECTURE.md](./ARCHITECTURE.md) - System components and technology stack
2. `README.md` in project root - Environment variables and configuration

## Documentation Standards

When updating documentation:

1. No icons or emojis
2. Clear, concise language
3. Code examples where helpful
4. Keep content focused and avoid duplication
5. Update this README when adding new documentation files

## Related Files

- `README.md` (project root) - Quick start guide and setup instructions
- `.kiro/steering/` - Kiro AI steering files (coding rules for AI assistant)
