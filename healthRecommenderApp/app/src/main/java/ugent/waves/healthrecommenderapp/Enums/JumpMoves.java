package ugent.waves.healthrecommenderapp.Enums;

//todo: kijk of numering juist
public enum JumpMoves {
    FAST(0),
    SLOW(1),
    SIDE_SWING(2),
    FORWARD_180(3),
    CROSS_OVER(4),
    OTHER(-1);

    private int jumpIndex;

    private JumpMoves(int i) { this.jumpIndex = i; }

    public static JumpMoves getJump(int i) {
        for (JumpMoves j : JumpMoves.values()) {
            if (j.jumpIndex == i) return j;
        }
        throw new IllegalArgumentException("Jump Not found");
    }

    public int getValue() {
        return jumpIndex;
    }
}
