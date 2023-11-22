package net.bernhardbmx.discord;

public class RPCCache {
    private final String state;
    private final String details;
    private final String smallImgKey;
    private final String smallImgText;

    public RPCCache(String details, String state, String smallImgKey, String smallImgText) {
        this.details = details;
        this.state = state;
        this.smallImgKey = smallImgKey;
        this.smallImgText = smallImgText;
    }

    public String getDetails() {
        return details;
    }

    public String getState() {
        return state;
    }

    public String getSmallImgKey() {
        return smallImgKey;
    }

    public String getSmallImgText() {
        return smallImgText;
    }
}
