package org.server.scrcpy;

import org.server.scrcpy.util.Workarounds;

import java.io.IOException;

public final class Server {

    private static String ip = null;

    private Server() {
        // not instantiable
    }

    private static void scrcpy(Options options) throws IOException {
        Workarounds.apply();  // init content

        ip = options.getIp();
        final Device device = new Device(options);

        // --turn-screen-off (-S): power the physical display off at start;
        // the mirror capture keeps streaming. Restored on exit.
        final boolean turnScreenOff = options.isTurnScreenOff();
        if (turnScreenOff) {
            if (device.setScreenPowerMode(false)) {
                Ln.i("Screen turned off (-S)");
            } else {
                Ln.w("Could not turn screen off (-S not supported on this device)");
            }
        }

        // --keep-active: simulate user activity periodically so the device
        // does not fall asleep, without changing any global power setting.
        Thread keepActiveThread = null;
        if (options.isKeepActive()) {
            keepActiveThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            Thread.sleep(10000);
                            device.userActivity();
                        }
                    } catch (InterruptedException e) {
                        // stopped
                    }
                }
            }, "keep-active");
            keepActiveThread.setDaemon(true);
            keepActiveThread.start();
            Ln.i("Keep-active started");
        }

        try (DroidConnection connection = DroidConnection.open(ip)) {
            ScreenEncoder screenEncoder = new ScreenEncoder(options);

            // asynchronous
            startEventController(device, connection, screenEncoder);

            try {
                // synchronous
                screenEncoder.streamScreen(options, device, connection.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                // this is expected on close
                Ln.d("Screen streaming stopped");

            }
        } finally {
            if (keepActiveThread != null) {
                keepActiveThread.interrupt();
            }
            if (turnScreenOff) {
                device.setScreenPowerMode(true);
                Ln.i("Screen turned back on");
            }
        }
    }

    private static void startEventController(final Device device, final DroidConnection connection, ScreenEncoder screenEncoder) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new EventController(device, connection, screenEncoder).control();
                } catch (IOException e) {
                    // this is expected on close
                    Ln.d("Event controller stopped");
                }
            }
        }).start();
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private static Options createOptions(String... args) {
//        Options options = new Options();
//
//        if (args.length < 1) {
//            return options;
//        }
//        ip = String.valueOf(args[0]);
//
//
//        if (args.length < 2) {
//            return options;
//        }
//        int maxSize = Integer.parseInt(args[1]) & ~7; // multiple of 8
//        options.setMaxSize(maxSize);
//
//        if (args.length < 3) {
//            return options;
//        }
//        int bitRate = Integer.parseInt(args[2]);
//        options.setBitRate(bitRate);
//
//        if (args.length < 4) {
//            return options;
//        }
//        // use "adb forward" instead of "adb tunnel"? (so the server must listen)
//        boolean tunnelForward = Boolean.parseBoolean(args[3]);
//        options.setTunnelForward(tunnelForward);
//        return options;

        return Options.createOptions(args);
    }

    public static void main(String... args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Ln.e("Exception on thread " + t, e);
            }
        });

        try {
            Process cmd = Runtime.getRuntime().exec("rm /data/local/tmp/scrcpy-server.jar");
            cmd.waitFor();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        Options options = createOptions(args);
        scrcpy(options);
    }
}

