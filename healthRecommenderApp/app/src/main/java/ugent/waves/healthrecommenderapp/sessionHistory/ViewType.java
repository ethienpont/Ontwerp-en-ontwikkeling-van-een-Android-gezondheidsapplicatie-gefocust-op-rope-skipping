package ugent.waves.healthrecommenderapp.sessionHistory;

interface ViewType {

     int LINE = 0;
     int ITEM = 1;

    int getViewType();

    /*
    enum ViewTypeEnum{
        LINE(0),
        ITEM(1);

        private int viewIndex;

        private ViewTypeEnum(int i) { this.viewIndex = i; }

        public static ViewTypeEnum getView(int i) {
            for (ViewTypeEnum j : ViewTypeEnum.values()) {
                if (j.viewIndex == i) return j;
            }
            throw new IllegalArgumentException("view Not found");
        }

        public int getValue() {
            return viewIndex;
        }
    }*/
}
