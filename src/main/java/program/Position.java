package program;

class Position {
   int y;
   int x;

    Position(int y, int x) {
        this.y = y;
        this.x = x;
    }

    @Override
    public String toString() {
        return "y:" + y + "  x:" + x;
    }
}
