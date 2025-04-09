package SoloTech.StockFlow.common.cache;

public enum CacheType {
    READ("READ"),
    WRITE("UPDATE"),
    DELETE("DELETE");

    private final String label;

    CacheType(String label){
        this.label = label;
    }

    public String getLabel(){
        return label;
    }
}

