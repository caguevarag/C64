import java.util.ArrayList;

public class Square {
    final int i, j;
    private ArrayList<Square> neighbors;
    private Square[] freeNeighbors;
    private int numFreeNeighbors, neighbor;

    private int val;

    public Square(int i, int j) {
        this.i = i;
        this.j = j;
        neighbors = new ArrayList<>();
        freeNeighbors = new Square[8];
    }

    public int getVal() {
        return val;
    }

    public void setVal(int i) {
        val = i;
    }

    public int getActualJump(){
        return neighbor;
    }

    public int getNumFreeNeighbors() {
        return numFreeNeighbors;
    }

    public boolean checkLoop(){
        if(this.val==64)
            for(Square sq:neighbors)
                if(sq.val == 1)
                    return true;
        return false;
    }

    public void addNeighbors(Square[][] board) {
        int[][] jumps = { { -2, -1 }, { -2, 1 }, { -1, -2 }, { -1, 2 }, { 1, -2 }, { 1, 2 }, { 2, -1 }, { 2, 1 } };
        for (int[] jump : jumps) {
            int i = this.i + jump[0];
            int j = this.j + jump[1];
            if (i >= 0 && i < 8 && j >= 0 && j < 8)
                neighbors.add(board[i][j]);
        }
    }

    public void setFreeNeighbors() {
        numFreeNeighbors = 0;
        neighbor = -1;
        for (int i = 0; i < neighbors.size(); i++) {
            Square nb = neighbors.get(i);
            if (nb.val == 0)
                freeNeighbors[numFreeNeighbors++] = nb;
        }
    }

    public Square nextNeighbor() {
        neighbor++;
        if (neighbor < numFreeNeighbors)
            return freeNeighbors[neighbor];

        return null;
    }

    public double getPartialProgress() {
        return (double) neighbor / numFreeNeighbors;
    }
}