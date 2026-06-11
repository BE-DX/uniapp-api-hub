package com.uniapp.apihub.module.auth;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginSessionNoticeService {

    private static final long NOTICE_TTL_SECONDS = 86400;

    private final Map<String, NoticeEntry> notices = new ConcurrentHashMap<>();

    public void put(String token, Map<String, Object> notice) {
        if (token == null || token.isEmpty()) {
            return;
        }
        notices.put(token, new NoticeEntry(notice, LocalDateTime.now().plusSeconds(NOTICE_TTL_SECONDS)));
    }

    public Map<String, Object> get(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        NoticeEntry entry = notices.get(token);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt.isBefore(LocalDateTime.now())) {
            notices.remove(token);
            return null;
        }
        return entry.notice;
    }

    private static class NoticeEntry {
        private final Map<String, Object> notice;
        private final LocalDateTime expireAt;

        private NoticeEntry(Map<String, Object> notice, LocalDateTime expireAt) {
            this.notice = notice;
            this.expireAt = expireAt;
        }
    }
}
