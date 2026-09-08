package org.server.scrcpy;

public class Options {

    public static final String CODEC_H264 = "h264";
    public static final String CODEC_H265 = "h265";
    public static final String DEFAULT_ENCODER = "-";

    private String ip;
    private int maxSize;
    private int bitRate;
    private boolean tunnelForward;
    private boolean enableAudioForward = true;
    private String videoCodec = CODEC_H264;
    private String videoEncoder = DEFAULT_ENCODER;
    private int frameRate = 60;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isEnableAudioForward() {
        return enableAudioForward;
    }

    public void setEnableAudioForward(boolean enableAudioForward) {
        this.enableAudioForward = enableAudioForward;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public boolean isTunnelForward() {
        return tunnelForward;
    }

    public void setTunnelForward(boolean tunnelForward) {
        this.tunnelForward = tunnelForward;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = normalizeCodec(videoCodec);
    }

    public String getVideoEncoder() {
        return videoEncoder;
    }

    public void setVideoEncoder(String videoEncoder) {
        if (videoEncoder == null || videoEncoder.isEmpty() || DEFAULT_ENCODER.equals(videoEncoder)) {
            this.videoEncoder = DEFAULT_ENCODER;
        } else {
            this.videoEncoder = videoEncoder.trim();
        }
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate > 0 ? frameRate : 60;
    }

    public String getVideoMimeType() {
        return mimeForCodec(videoCodec);
    }

    public boolean hasCustomEncoder() {
        return videoEncoder != null && !videoEncoder.isEmpty() && !DEFAULT_ENCODER.equals(videoEncoder);
    }

    public static String normalizeCodec(String codec) {
        if (codec == null) {
            return CODEC_H264;
        }
        String value = codec.trim().toLowerCase();
        if ("h265".equals(value) || "hevc".equals(value)) {
            return CODEC_H265;
        }
        return CODEC_H264;
    }

    public static String mimeForCodec(String codec) {
        return CODEC_H265.equals(normalizeCodec(codec)) ? "video/hevc" : "video/avc";
    }

    /**
     * Restore options from args. Extra args are optional for backward compatibility.
     */
    public static Options createOptions(String[] args) {
        Options options = new Options();
        options.ip = args[0];
        options.maxSize = Integer.parseInt(args[1]) & ~7;
        options.bitRate = Integer.parseInt(args[2]);
        options.tunnelForward = Boolean.parseBoolean(args[3]);
        options.enableAudioForward = Boolean.parseBoolean(args[4]);
        if (args.length > 5) {
            options.setVideoCodec(args[5]);
        }
        if (args.length > 6) {
            options.setVideoEncoder(args[6]);
        }
        if (args.length > 7) {
            options.setFrameRate(Integer.parseInt(args[7]));
        }
        return options;
    }

    public String[] optionsToArgs() {
        return new String[]{
                ip,
                Long.toString(maxSize),
                Long.toString(bitRate),
                String.valueOf(tunnelForward),
                String.valueOf(enableAudioForward),
                videoCodec,
                hasCustomEncoder() ? videoEncoder : DEFAULT_ENCODER,
                Integer.toString(frameRate)
        };
    }
}
