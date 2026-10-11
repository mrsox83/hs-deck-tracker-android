package com.stroexd.hsdecktracker.logs;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;

/** Adapted from the R0-pinned v0.4 LogReader.java; namespace/class and list-error wording changed. */
public final class RestrictedLogReader extends ILogReader.Stub {
    private static final File ROOT = new File("/storage/emulated/0/Android/data/com.blizzard.wtcg.hearthstone/files/Logs");
    public RestrictedLogReader() {}

    @Override public String[] listSessions() throws RemoteException {
        String[] names = ROOT.list((dir, name) -> name.matches("Hearthstone_[0-9_]+") && new File(dir, name).isDirectory());
        if (names == null) throw new RemoteException("Cannot list Hearthstone logs; shell uid=" + android.os.Process.myUid());
        Arrays.sort(names, Collections.reverseOrder());
        return names;
    }

    @Override public ParcelFileDescriptor openLog(String session) throws RemoteException {
        try {
            if (session == null || !session.matches("Hearthstone_[0-9_]+")) throw new SecurityException("Invalid session name");
            File file = new File(ROOT, session + "/Power.log");
            if (!file.getCanonicalPath().equals(ROOT.getCanonicalPath() + "/" + session + "/Power.log"))
                throw new SecurityException("Unexpected canonical path");
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (Exception error) {
            throw new RemoteException(error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    @Override public void destroy() { System.exit(0); }
}
