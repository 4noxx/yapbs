package info.rbuck.billiardscoreboard.domain

enum class GameType(val displayName: String, val isStraightPool: Boolean) {
    EIGHT_BALL("8-Ball", false),
    NINE_BALL("9-Ball", false),
    TEN_BALL("10-Ball", false),
    STRAIGHT_POOL("14.1 Straight Pool", true),
}
