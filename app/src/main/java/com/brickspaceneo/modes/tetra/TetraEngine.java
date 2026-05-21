package com.brickspaceneo.modes.tetra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import com.brickspaceneo.common.ComboSystem;

public class TetraEngine {
    public static final int COLS = 10;
    public static final int ROWS = 20;

    private static final int[][][][] SHAPES = new int[][][][]{
            {
                    {{0, 1}, {1, 1}, {2, 1}, {3, 1}},
                    {{2, 0}, {2, 1}, {2, 2}, {2, 3}},
                    {{0, 2}, {1, 2}, {2, 2}, {3, 2}},
                    {{1, 0}, {1, 1}, {1, 2}, {1, 3}}
            },
            {
                    {{0, 0}, {0, 1}, {1, 1}, {2, 1}},
                    {{1, 0}, {2, 0}, {1, 1}, {1, 2}},
                    {{0, 1}, {1, 1}, {2, 1}, {2, 2}},
                    {{1, 0}, {1, 1}, {0, 2}, {1, 2}}
            },
            {
                    {{2, 0}, {0, 1}, {1, 1}, {2, 1}},
                    {{1, 0}, {1, 1}, {1, 2}, {2, 2}},
                    {{0, 1}, {1, 1}, {2, 1}, {0, 2}},
                    {{0, 0}, {1, 0}, {1, 1}, {1, 2}}
            },
            {
                    {{1, 0}, {2, 0}, {1, 1}, {2, 1}},
                    {{1, 0}, {2, 0}, {1, 1}, {2, 1}},
                    {{1, 0}, {2, 0}, {1, 1}, {2, 1}},
                    {{1, 0}, {2, 0}, {1, 1}, {2, 1}}
            },
            {
                    {{1, 0}, {2, 0}, {0, 1}, {1, 1}},
                    {{1, 0}, {1, 1}, {2, 1}, {2, 2}},
                    {{1, 1}, {2, 1}, {0, 2}, {1, 2}},
                    {{0, 0}, {0, 1}, {1, 1}, {1, 2}}
            },
            {
                    {{1, 0}, {0, 1}, {1, 1}, {2, 1}},
                    {{1, 0}, {1, 1}, {2, 1}, {1, 2}},
                    {{0, 1}, {1, 1}, {2, 1}, {1, 2}},
                    {{1, 0}, {0, 1}, {1, 1}, {1, 2}}
            },
            {
                    {{0, 0}, {1, 0}, {1, 1}, {2, 1}},
                    {{2, 0}, {1, 1}, {2, 1}, {1, 2}},
                    {{0, 1}, {1, 1}, {1, 2}, {2, 2}},
                    {{1, 0}, {0, 1}, {1, 1}, {0, 2}}
            }
    };

    private static final int[] LINE_CLEAR_SCORE = new int[]{0, 120, 320, 560, 900};

    private final Random random = new Random(17L);
    private final int[][] board = new int[ROWS][COLS];
    private final ArrayList<Integer> bag = new ArrayList<>();
    private final ArrayList<LineBurst> lineBursts = new ArrayList<>();
    private final ComboSystem comboSystem = new ComboSystem();

    private Piece active;
    private Piece next;
    private float fallTimer;
    private float lockFlash;
    private int score;
    private int level;
    private int linesCleared;
    private boolean paused;
    private boolean gameOver;

    private static final float LOCK_DELAY = 0.5f;
    private static final int MAX_RESETS = 15;
    private boolean isGrounded;
    private float lockDelayTimer;
    private int lockResets;
    private float comboDisplayTimer;

    public TetraEngine() {
        restart();
    }

    public void restart() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                board[row][col] = 0;
            }
        }
        bag.clear();
        lineBursts.clear();
        score = 0;
        linesCleared = 0;
        paused = false;
        gameOver = false;
        fallTimer = 0f;
        lockFlash = 0f;
        isGrounded = false;
        lockDelayTimer = 0f;
        lockResets = 0;
        comboDisplayTimer = 0f;
        comboSystem.reset();
        next = new Piece(drawFromBag(), 3, 0, 0);
        spawnPiece();
    }

    public void update(float deltaSeconds) {
        if (paused || gameOver) {
            decayEffects(deltaSeconds);
            return;
        }

        decayEffects(deltaSeconds);
        
        if (active == null) return;

        boolean onGround = !fits(active.type, active.rotation, active.x, active.y + 1);

        if (onGround) {
            if (!isGrounded) {
                isGrounded = true;
                lockDelayTimer = LOCK_DELAY;
            }
            lockDelayTimer -= deltaSeconds;
            if (lockDelayTimer <= 0) {
                lockPiece();
            }
        } else {
            isGrounded = false;
            fallTimer += deltaSeconds;
            float interval = getFallInterval();
            while (fallTimer >= interval) {
                fallTimer -= interval;
                if (fits(active.type, active.rotation, active.x, active.y + 1)) {
                    active.y += 1;
                    lockResets = 0;
                } else {
                    break;
                }
            }
            if (!fits(active.type, active.rotation, active.x, active.y + 1)) {
                isGrounded = true;
                lockDelayTimer = LOCK_DELAY;
            }
        }
    }

    private void decayEffects(float deltaSeconds) {
        lockFlash = Math.max(0f, lockFlash - deltaSeconds * 4f);
        comboDisplayTimer = Math.max(0f, comboDisplayTimer - deltaSeconds);
        for (int i = lineBursts.size() - 1; i >= 0; i--) {
            LineBurst burst = lineBursts.get(i);
            burst.age += deltaSeconds;
            if (burst.age > 1.25f) {
                lineBursts.remove(i);
            }
        }
    }

    public boolean move(int dx) {
        if (!canControl()) {
            return false;
        }
        if (fits(active.type, active.rotation, active.x + dx, active.y)) {
            active.x += dx;
            if (isGrounded && lockResets < MAX_RESETS) {
                lockDelayTimer = LOCK_DELAY;
                lockResets++;
            }
            return true;
        }
        return false;
    }

    public boolean rotate() {
        if (!canControl()) {
            return false;
        }
        int targetRotation = (active.rotation + 1) % 4;
        int[] kicks = new int[]{0, -1, 1, -2, 2};
        for (int kick : kicks) {
            if (fits(active.type, targetRotation, active.x + kick, active.y)) {
                active.rotation = targetRotation;
                active.x += kick;
                if (isGrounded && lockResets < MAX_RESETS) {
                    lockDelayTimer = LOCK_DELAY;
                    lockResets++;
                }
                return true;
            }
        }
        return false;
    }

    public boolean softDrop() {
        if (!canControl()) {
            return false;
        }
        if (fits(active.type, active.rotation, active.x, active.y + 1)) {
            active.y += 1;
            score += 2;
            fallTimer = 0f;
            lockResets = 0;
            return true;
        }
        return false;
    }

    public void hardDrop() {
        if (!canControl()) {
            return;
        }
        int traveled = 0;
        while (fits(active.type, active.rotation, active.x, active.y + 1)) {
            active.y += 1;
            traveled++;
        }
        score += traveled * 6;
        lockPiece();
    }

    public void togglePause() {
        if (!gameOver) {
            paused = !paused;
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getScore() {
        return score;
    }

    public int getDisplayLevel() {
        return (linesCleared % 96) / 6;
    }

    public int getLinesCleared() {
        return linesCleared;
    }

    public int getLinesInCycle() {
        return linesCleared % 96;
    }

    public float getCycleProgress() {
        return (linesCleared % 96) / 96.0f;
    }

    public int getInternalStage() {
        return linesCleared / 6;
    }

    public int getLinesToNextLevel() {
        return 6 - (linesCleared % 6);
    }

    public float getFallInterval() {
        int stage = getInternalStage();
        return Math.max(0.08f, 0.78f - stage * 0.045f); // Slightly adjusted for better scaling
    }

    public float getProgressToFall() {
        return Math.min(1f, fallTimer / getFallInterval());
    }

    public float getLockFlash() {
        return lockFlash;
    }

    public int getCell(int row, int col) {
        return board[row][col];
    }

    public Piece getActive() {
        return active;
    }

    public Piece getNext() {
        return next;
    }

    public List<int[]> getGhostCells() {
        ArrayList<int[]> cells = new ArrayList<>();
        if (active == null) {
            return cells;
        }
        int ghostY = active.y;
        while (fits(active.type, active.rotation, active.x, ghostY + 1)) {
            ghostY++;
        }
        int[][] shape = SHAPES[active.type][active.rotation];
        for (int[] cell : shape) {
            cells.add(new int[]{active.x + cell[0], ghostY + cell[1]});
        }
        return cells;
    }

    public List<int[]> getActiveCells() {
        ArrayList<int[]> cells = new ArrayList<>();
        if (active == null) {
            return cells;
        }
        int[][] shape = SHAPES[active.type][active.rotation];
        for (int[] cell : shape) {
            cells.add(new int[]{active.x + cell[0], active.y + cell[1]});
        }
        return cells;
    }

    public List<LineBurst> getLineBursts() {
        return Collections.unmodifiableList(lineBursts);
    }

    public ComboSystem getComboSystem() {
        return comboSystem;
    }

    public float getComboDisplayTimer() {
        return comboDisplayTimer;
    }

    public static int[][] getShapeCells(int type, int rotation) {
        return SHAPES[type][rotation];
    }

    private void lockPiece() {
        int[][] shape = SHAPES[active.type][active.rotation];
        for (int[] cell : shape) {
            int boardX = active.x + cell[0];
            int boardY = active.y + cell[1];
            if (boardY < 0) {
                gameOver = true;
                lockFlash = 1f;
                spawnGameOverBurst();
                return;
            }
            board[boardY][boardX] = active.type + 1;
        }
        clearLines();
        lockFlash = 1f;
        spawnPiece();
    }

    private void spawnPiece() {
        active = next;
        active.x = 3;
        active.y = -1;
        active.rotation = 0;
        next = new Piece(drawFromBag(), 3, 0, 0);
        fallTimer = 0f;
        isGrounded = false;
        lockDelayTimer = 0f;
        lockResets = 0;
        if (!fits(active.type, active.rotation, active.x, active.y)) {
            gameOver = true;
            spawnGameOverBurst();
        }
    }

    private void clearLines() {
        ArrayList<Integer> cleared = new ArrayList<>();
        for (int row = 0; row < ROWS; row++) {
            boolean full = true;
            for (int col = 0; col < COLS; col++) {
                if (board[row][col] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                cleared.add(row);
            }
        }

        if (cleared.isEmpty()) {
            comboSystem.processLinesCleared(0);
            comboDisplayTimer = 0f;
            return;
        }

        for (int rowIndex : cleared) {
            int[] colors = new int[COLS];
            for (int col = 0; col < COLS; col++) {
                colors[col] = board[rowIndex][col];
            }
            lineBursts.add(new LineBurst(rowIndex, colors, false));
        }

        for (int i = 0; i < cleared.size(); i++) {
            int clearRow = cleared.get(i);
            for (int row = clearRow; row > 0; row--) {
                System.arraycopy(board[row - 1], 0, board[row], 0, COLS);
            }
            for (int col = 0; col < COLS; col++) {
                board[0][col] = 0;
            }
        }

        int linesCount = cleared.size();
        linesCleared += linesCount;
        
        int scoreToAdd = comboSystem.processLinesCleared(linesCount);
        score += scoreToAdd * (getDisplayLevel() + 1); // Use cycle-aware display level for score bonus

        if (comboSystem.getComboCount() >= 2) {
            comboDisplayTimer = 1.0f; // Show indicator for 1 second
        }
    }

    private void spawnGameOverBurst() {
        for (int row = 0; row < ROWS; row++) {
            boolean any = false;
            int[] colors = new int[COLS];
            for (int col = 0; col < COLS; col++) {
                colors[col] = board[row][col];
                any |= colors[col] != 0;
            }
            if (any) {
                lineBursts.add(new LineBurst(row, colors, true));
            }
        }
    }

    private int drawFromBag() {
        if (bag.isEmpty()) {
            for (int i = 0; i < SHAPES.length; i++) {
                bag.add(i);
            }
            Collections.shuffle(bag, random);
        }
        return bag.remove(bag.size() - 1);
    }

    private boolean fits(int type, int rotation, int offsetX, int offsetY) {
        int[][] shape = SHAPES[type][rotation];
        for (int[] cell : shape) {
            int boardX = offsetX + cell[0];
            int boardY = offsetY + cell[1];
            if (boardX < 0 || boardX >= COLS || boardY >= ROWS) {
                return false;
            }
            if (boardY >= 0 && board[boardY][boardX] != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean canControl() {
        return !paused && !gameOver && active != null;
    }

    public static final class Piece {
        public final int type;
        public int x;
        public int y;
        public int rotation;

        private Piece(int type, int x, int y, int rotation) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
        }
    }

    public static final class LineBurst {
        public final int row;
        public final int[] colors;
        public final boolean catastrophic;
        public float age;
        public boolean emitted;

        private LineBurst(int row, int[] colors, boolean catastrophic) {
            this.row = row;
            this.colors = colors;
            this.catastrophic = catastrophic;
        }
    }
}
