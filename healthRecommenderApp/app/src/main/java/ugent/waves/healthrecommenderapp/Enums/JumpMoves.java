package ugent.waves.healthrecommenderapp.Enums;

//Rope skipping moves
public enum JumpMoves {
    CROSS_OVER(0),
    FAST(1),
    RUN(2),
    SLOW(3),
    SIDE_SWING(4);

    private int jumpIndex;

    private JumpMoves(int i) { this.jumpIndex = i; }

    public static JumpMoves getJump(int i) {
        for (JumpMoves j : JumpMoves.values()) {
            if (j.jumpIndex == i) return j;
        }
        throw new IllegalArgumentException("Jump Not found");
    }

    public static String getJumpName(int i) {
        if(i == 0){
            return "Cross over";
        } else if(i == 1){
            return "Jump fast";
        } else if(i == 2){
            return "Jump run";
        } else if(i == 3){
            return "Jump slow";
        } else if(i == 4){
            return "Side swing";
        }
        return "Unknown";
    }

    public int getValue() {
        return jumpIndex;
    }
}
