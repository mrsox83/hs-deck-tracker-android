package com.stroexd.hsdecktracker.logs;
import android.os.ParcelFileDescriptor;
// Protocol from pinned HS Post-match Exporter v0.4; no arbitrary paths or write/shell API.
interface ILogReader {
    String[] listSessions() = 0;
    ParcelFileDescriptor openLog(String session) = 1;
    void destroy() = 16777114;
}
