package com.nitrotech.api.domain.user.usecase;

import com.nitrotech.api.domain.user.dto.UserImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImportAdminUsersUseCase {

    private final CreateAdminUserUseCase createAdminUserUseCase;

    public UserImportResult execute(MultipartFile file) {
        MapResult result = new MapResult();
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            result.fail(1, "Không đọc được file");
            return result.toResult();
        }

        String[] lines = content.split("\\R");
        char delimiter = lines.length > 0 ? delimiter(lines[0]) : ',';
        if (lines.length == 0 || !validHeader(parseCsvLine(lines[0], delimiter))) {
            result.fail(1, "File không đúng mẫu import: name,email,phone,roles,status");
            return result.toResult();
        }
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) {
                continue;
            }
            List<String> cols = parseCsvLine(line, delimiter);
            String name = col(cols, 0);
            String email = col(cols, 1);
            String phone = col(cols, 2);
            String rawRoles = col(cols, 3);
            String status = col(cols, 4);
            if (name.isBlank() || email.isBlank()) {
                result.fail(i + 1, "Thiếu name hoặc email");
                continue;
            }
            if (!email.contains("@")) {
                result.fail(i + 1, "Email không hợp lệ");
                continue;
            }
            try {
                createAdminUserUseCase.execute(
                        name,
                        email,
                        phone,
                        status.isBlank() ? "active" : status,
                        roles(rawRoles)
                );
                result.created++;
            } catch (Exception e) {
                result.fail(i + 1, e.getMessage());
            }
        }
        return result.toResult();
    }

    private boolean validHeader(List<String> cols) {
        return cols.size() >= 5
                && "name".equals(cleanHeader(cols.get(0)))
                && "email".equals(cols.get(1).trim())
                && "phone".equals(cols.get(2).trim())
                && "roles".equals(cols.get(3).trim())
                && "status".equals(cols.get(4).trim());
    }

    private char delimiter(String headerLine) {
        long tabs = headerLine.chars().filter(c -> c == '\t').count();
        long semicolons = headerLine.chars().filter(c -> c == ';').count();
        long commas = headerLine.chars().filter(c -> c == ',').count();
        if (tabs >= commas && tabs >= semicolons && tabs > 0) return '\t';
        if (semicolons >= commas && semicolons > 0) return ';';
        return ',';
    }

    private String cleanHeader(String value) {
        return value == null ? "" : value.replace("\uFEFF", "").trim();
    }

    private String col(List<String> cols, int index) {
        return index >= 0 && index < cols.size() ? cols.get(index).trim() : "";
    }

    private List<String> parseCsvLine(String line, char delimiter) {
        List<String> cols = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == delimiter && !quoted) {
                cols.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cols.add(current.toString());
        return cols;
    }

    private Set<String> roles(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of("customer");
        }
        return Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static class MapResult {
        int created;
        final LinkedHashMap<Integer, String> failedReasons = new LinkedHashMap<>();

        void fail(int row, String reason) {
            failedReasons.put(row, reason);
        }

        UserImportResult toResult() {
            return new UserImportResult(created, failedReasons.size(), List.copyOf(failedReasons.keySet()), failedReasons);
        }
    }
}
