package ugent.waves.healthrecommenderapp.Enums;

//todo: kijk of numering juist
public enum JumpMoves {
    FAST(0),
    SLOW(1),
    SIDE_SWING(2),
    FORWARD_180(3),
    CROSS_OVER(4),
    MISTAKE(5),
    OTHER(-1);

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
            return "Jump fast";
        } else if(i == 1){
            return "Jump slow";
        } else if(i == 2){
            return "Side swing";
        } else if(i == 3){
            return "Forward 180";
        } else if(i == 4){
            return "Cross over";
        } else if(i == 5){
            return "Mistake";
        }
        return "Other";
    }

    public int getValue() {
        return jumpIndex;
    }
}
