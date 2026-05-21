package com.brickspaceneo.modes.columns;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ColumnsEngine {
    public static final int COLS = 10;
    public static final int ROWS = 20;
    public static final int COLOR_COUNT = 6;

    public static class Piece {
        public int color;
        public boolean matched;
        public Piece(int color) { this.color = color; }
    }

    public static class MatchedCluster {
        public List<int[]> positions;
        public int color;
        public MatchedCluster(List<int[]> positions, int color) {
            this.positions = new ArrayList<>(positions);
            this.color = color;
        }
    }

    private final Piece[][] grid = new Piece[ROWS][COLS];
    private final List<MatchedCluster> pendingVisuals = new ArrayList<>();
    private final int[] fallingColumn = new int[4];
    private final int[] nextColumn = new int[4];
    private int colX;
    private float colY;
    
    private int score;
    private int combo;
    private int level = 1;
    private int totalCleared;
    private boolean gameOver;
    private boolean paused;
    private boolean isResolving; // Waiting for matches/falls
    
    private float moveTimer = 0f;
    private float resolveTimer = 0f;
    private final Random random = new Random();

    public ColumnsEngine() {
        // Initial next column
        generateNext();
        resetGame();
    }

    public void resetGame() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = null;
            }
        }
        score = 0;
        combo = 1;
        level = 1;
        totalCleared = 0;
        gameOver = false;
        paused = false;
        isResolving = false;
        generateNext();
        spawnColumn();
    }

    private void generateNext() {
        for (int i = 0; i < 4; i++) {
            nextColumn[i] = random.nextInt(COLOR_COUNT) + 1;
        }
    }

    private void spawnColumn() {
        System.arraycopy(nextColumn, 0, fallingColumn, 0, 4);
        generateNext();
        colX = COLS / 2;
        colY = -4f; // Start above (size 4)

        // Check if spawn position is blocked
        if (grid[0][colX] != null) {
            gameOver = true;
        }
    }

    public void update(float delta) {
        if (gameOver || paused) return;

        if (isResolving) {
            resolveTimer += delta;
            if (resolveTimer >= 0.15f) { // Faster resolution
                resolveTimer = 0f;
                if (!applyGravity()) {
                    if (!checkMatches()) {
                        isResolving = false;
                        combo = 1;
                        spawnColumn();
                    }
                }
            }
            return;
        }

        moveTimer += delta;
        float speed = getSpeed();
        if (moveTimer >= speed) {
            moveTimer = 0f;
            if (!moveDown()) {
                lockColumn();
            }
        }
    }

    public boolean move(int dx) {
        if (isResolving || gameOver || paused) return false;
        int newX = colX + dx;
        if (newX < 0 || newX >= COLS) return false;
        
        // Check collision at all 4 vertical spots
        int topRow = (int)Math.floor(colY);
        for (int i = 0; i < 4; i++) {
            int row = topRow + i;
            if (row >= 0 && row < ROWS && grid[row][newX] != null) return false;
        }
        
        colX = newX;
        return true;
    }

    public void rotate() {
        if (isResolving || gameOver || paused) return;
        // ABCD -> DABC
        int temp = fallingColumn[3];
        fallingColumn[3] = fallingColumn[2];
        fallingColumn[2] = fallingColumn[1];
        fallingColumn[1] = fallingColumn[0];
        fallingColumn[0] = temp;
    }

    public boolean moveDown() {
        float newY = colY + 1;
        int bottomRow = (int)Math.floor(newY) + 3;
        
        if (bottomRow >= ROWS || (bottomRow >= 0 && grid[bottomRow][colX] != null)) {
            return false;
        }
        colY += 1;
        return true;
    }

    public void fastDrop() {
        while (moveDown()) {
            // score += 1; // Bonus for drop
        }
        lockColumn();
    }

    private void lockColumn() {
        int topRow = (int)Math.floor(colY);
        for (int i = 0; i < 4; i++) {
            int row = topRow + i;
            if (row >= 0 && row < ROWS) {
                grid[row][colX] = new Piece(fallingColumn[i]);
            }
        }
        isResolving = true;
        resolveTimer = 0.1f;
    }

    private boolean applyGravity() {
        boolean changed = false;
        for (int c = 0; c < COLS; c++) {
            for (int r = ROWS - 1; r > 0; r--) {
                if (grid[r][c] == null && grid[r-1][c] != null) {
                    grid[r][c] = grid[r-1][c];
                    grid[r-1][c] = null;
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean checkMatches() {
        boolean[][] toClear = new boolean[ROWS][COLS];
        boolean[][] visited = new boolean[ROWS][COLS];
        boolean foundMatch = false;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == null || visited[r][c]) continue;

                int color = grid[r][c].color;
                List<int[]> cluster = new ArrayList<>();
                findCluster(r, c, color, visited, cluster);

                if (cluster.size() >= 4) {
                    foundMatch = true;
                    pendingVisuals.add(new MatchedCluster(cluster, color));
                    for (int[] pos : cluster) {
                        toClear[pos[0]][pos[1]] = true;
                    }
                }
            }
        }

        if (foundMatch) {
            int clearedCount = 0;
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    if (toClear[r][c]) {
                        grid[r][c] = null;
                        clearedCount++;
                    }
                }
            }
            
            // Score with combo multiplier
            score += clearedCount * 10 * combo * level;
            totalCleared += clearedCount;
            // New level every 32 blocks, max 24, loops back to 1
            level = ((totalCleared / 32) % 24) + 1;
            combo++; // Increment combo for next phase
            return true;
        }
        
        return false;
    }

    private void findCluster(int r, int c, int color, boolean[][] visited, List<int[]> cluster) {
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return;
        if (visited[r][c] || grid[r][c] == null || grid[r][c].color != color) return;

        visited[r][c] = true;
        cluster.add(new int[]{r, c});

        // 8 directions: H, V, and Diagonals
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                findCluster(r + dr, c + dc, color, visited, cluster);
            }
        }
    }

    private float getSpeed() {
        // Base 0.8s. Levels 1-3 linear. After 3, exponential 1.2x per level.
        double speed;
        if (level <= 3) {
            speed = 0.8 - (level - 1) * 0.1; // 0.8, 0.7, 0.6
        } else {
            speed = 0.6 / Math.pow(1.2, level - 3);
        }
        return (float) Math.max(0.05, speed);
    }

    public int getGhostRow() {
        if (gameOver || isResolving) return -1;
        int ghostR = (int) colY;
        while (ghostR + 4 < ROWS) {
            boolean blocked = false;
            if (grid[ghostR + 4][colX] != null) {
                blocked = true;
            }
            if (blocked) break;
            ghostR++;
        }
        return ghostR;
    }

    public List<MatchedCluster> getAndClearVisuals() {
        List<MatchedCluster> copy = new ArrayList<>(pendingVisuals);
        pendingVisuals.clear();
        return copy;
    }

    public Piece[][] getGrid() { return grid; }
    public int[] getFallingColumn() { return fallingColumn; }
    public int[] getNextColumn() { return nextColumn; }
    public int getColX() { return colX; }
    public float getColY() { return colY; }
    public int getScore() { return score; }
    public int getCombo() { return combo; }
    public int getLevel() { return level; }
    public int getTotalCleared() { return totalCleared; }
    public boolean isGameOver() { return gameOver; }
    public boolean isPaused() { return paused; }
    public void togglePause() { paused = !paused; }
    public boolean isResolving() { return isResolving; }
}
