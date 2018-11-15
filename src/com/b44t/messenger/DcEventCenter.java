package com.b44t.messenger;

import android.os.AsyncTask;

import org.thoughtcrime.securesms.util.Util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;

public class DcEventCenter {
    private Hashtable<Integer, ArrayList<DcEventDelegate>> allObservers = new Hashtable<>();
    private final Object LOCK = new Object();

    public interface DcEventDelegate {
        void handleEvent(int eventId, Object data1, Object data2);
        default boolean runOnMain() {
            return true;
        }
    }

    /**
     * @deprecated use addObserver(int, DcEventDelegate) instead.
     */
    @Deprecated
    public void addObserver(DcEventDelegate observer, int eventId) {
        addObserver(eventId, observer);
    }

    public void addObserver(int eventId, DcEventDelegate observer) {
        synchronized (LOCK) {
            ArrayList<DcEventDelegate> idObservers = allObservers.get(eventId);
            if (idObservers == null) {
                allObservers.put(eventId, (idObservers = new ArrayList<>()));
            }
            idObservers.add(observer);
        }
    }

    public void removeObserver(int eventId, DcEventDelegate observer) {
        synchronized (LOCK) {
            ArrayList<DcEventDelegate> idObservers = allObservers.get(eventId);
            if (idObservers != null) {
                idObservers.remove(observer);
            }
        }
    }

    public void removeObservers(DcEventDelegate observer) {
        synchronized (LOCK) {
            for(Integer eventId : allObservers.keySet()) {
                ArrayList<DcEventDelegate> idObservers = allObservers.get(eventId);
                if (idObservers != null) {
                    idObservers.remove(observer);
                }
            }
        }
    }

    public void sendToObservers(int eventId, Object data1, Object data2) {
        synchronized (LOCK) {
            ArrayList<DcEventDelegate> idObservers = allObservers.get(eventId);
            if (idObservers != null) {
                for (DcEventDelegate observer : idObservers) {
                    if(observer.runOnMain()) {
                        Util.runOnMain(() -> observer.handleEvent(eventId, data1, data2));
                    } else {
                        new BackgroundEventHandler(observer, eventId)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data1, data2);
                    }
                }
            }
        }
    }

    private static class BackgroundEventHandler extends AsyncTask<Object, Void, Void> {
        private final WeakReference<DcEventDelegate> asyncDelegate;
        private final int eventId;
        BackgroundEventHandler(DcEventDelegate delegate, int eventId) {
            asyncDelegate = new WeakReference<>(delegate);
            this.eventId = eventId;
        }
        @Override
        protected Void doInBackground(Object... data) {
            DcEventDelegate delegate = asyncDelegate.get();
            if(delegate != null) {
                delegate.handleEvent(eventId, data[0], data[1]);
            }
            return null;
        }
    }
}
