package com.brickspaceneo.modes.collapse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import com.brickspaceneo.common.ComboSystem;

public class CollapseEngine {
    public static final int COLS = 10;
    public static final int ROWS = 20;

    // 8 custom standard and irregular shapes matching the user's requested block types
    private static final int[][][][] SHAPES = new int[][][][]{
            // Shape 0: S-Shape (Green)
            {
                    {{1, 0}, {2, 0}, {0, 1}, {1, 1}},
                    {{0, 0}, {0, 1}, {1, 1}, {1, 2}},
                    {{1, 0}, {2, 0}, {0, 1}, {1, 1}},
                    {{0, 0}, {0, 1}, {1, 1}, {1, 2}}
            },
            // Shape 1: T-Shape (Purple)
            {
                    {{0, 0}, {1, 0}, {2, 0}, {1, 1}},
                    {{1, 0}, {0, 1}, {1, 1}, {1, 2}},
                    {{1, 0}, {0, 1}, {1, 1}, {2, 1}},
                    {{0, 0}, {0, 1}, {1, 1}, {0, 2}}
            },
            // Shape 2: L-Shape (Magenta)
            {
                    {{0, 0}, {1, 0}, {2, 0}, {0, 1}},
                    {{0, 0}, {1, 0}, {1, 1}, {1, 2}},
                    {{2, 0}, {0, 1}, {1, 1}, {2, 1}},
                    {{0, 0}, {0, 1}, {0, 2}, {1, 2}}
            },
            // Shape 3: O-Shape / Square (Blue)
            {
                    {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
                    {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
                    {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
                    {{0, 0}, {1, 0}, {0, 1}, {1, 1}}
            },
            // Shape 4: Z-Shape (Orange)
            {
                    {{0, 0}, {1, 0}, {1, 1}, {2, 1}},
                    {{1, 0}, {0, 1}, {1, 1}, {0, 2}},
                    {{0, 0}, {1, 0}, {1, 1}, {2, 1}},
                    {{1, 0}, {0, 1}, {1, 1}, {0, 2}}
            },
            // Shape 5: Plus / Cross (Cyan)
            {
                    {{1, 0}, {0, 1}, {1, 1}, {2, 1}, {1, 2}},
                    {{1, 0}, {0, 1}, {1, 1}, {2, 1}, {1, 2}},
                    {{1, 0}, {0, 1}, {1, 1}, {2, 1}, {1, 2}},
                    {{1, 0}, {0, 1}, {1, 1}, {2, 1}, {1, 2}}
            },
            // Shape 6: I-Shape / Bar (Orange/Yellow)
            {
                    {{0, 0}, {1, 0}, {2, 0}, {3, 0}},
                    {{0, 0}, {0, 1}, {0, 2}, {0, 3}},
                    {{0, 0}, {1, 0}, {2, 0}, {3, 0}},
                    {{0, 0}, {0, 1}, {0, 2}, {0, 3}}
            },
            // Shape 7: U-Shape (Lime/Yellow-green)
            {
                    {{0, 0}, {2, 0}, {0, 1}, {1, 1}, {2, 1}},
                    {{0, 0}, {1, 0}, {0, 1}, {0, 2}, {1, 2}},
                    {{0, 0}, {1, 0}, {2, 0}, {0, 1}, {2, 1}},
                    {{0, 0}, {1, 0}, {1, 1}, {0, 2}, {1, 2}}
            }
    };

    private final Random random = new Random();
    private final int[][] board = new int[ROWS][COLS];
    private final ArrayList<Integer> bag = new ArrayList<>();
    private final ArrayList<LineBurst> lineBursts = new ArrayList<>();
    private final ComboSystem comboSystem = new ComboSystem();

    private Piece active;
    private Piece next;
    
    private float fallTimer = 0f;
    private float risingRowTimer = 0f;
    
    private int score = 0;
    private int level = 1;
    private int linesCleared = 0;
    
    private boolean paused = false;
    private boolean gameOver = false;
    private boolean isResolving = false;
    private float resolveTimer = 0f;
    
    private static final float LOCK_DELAY = 0.5f;
    private static final int MAX_RESETS = 15;
    private boolean isGrounded = false;
    private float lockDelayTimer = 0f;
    private int lockResets = 0;
    private float comboDisplayTimer = 0f;

    public CollapseEngine() {
        restart();
    }

    public void restart() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                board[r][c] = 0;
            }
        }
        bag.clear();
        lineBursts.clear();
        score = 0;
        level = 1;
        linesCleared = 0;
        paused = false;
        gameOver = false;
        isResolving = false;
        fallTimer = 0f;
        risingRowTimer = 0f;
        isGrounded = false;
        lockDelayTimer = 0f;
        lockResets = 0;
        comboDisplayTimer = 0f;
        comboSystem.reset();
        
        next = new Piece(drawFromBag(), 4, 0, 0);
        spawnPiece();
    }

    public void update(float deltaSeconds) {
        if (paused || gameOver) {
            decayEffects(deltaSeconds);
            return;
        }

        decayEffects(deltaSeconds);

        if (isResolving) {
            resolveTimer += deltaSeconds;
            if (resolveTimer >= 0.12f) { // Step interval for animated cascading falls
                resolveTimer = 0f;
                
                // Step 1: Let gravity pull blocks down by 1 row
                if (!applyGravityStep()) {
                    // Step 2: If everything settled, check for filled lines
                    int cleared = checkAndClearFilledLines();
                    if (cleared == 0) {
                        // Cascade reaction ended completely! Reset resolution phase
                        isResolving = false;
                        if (active == null) {
                            spawnPiece();
                        }
                    }
                }
            }
            return;
        }

        if (active == null) return;

        // Continuous bottom rising pressure
        risingRowTimer += deltaSeconds;
        if (risingRowTimer >= getRisingRowInterval()) {
            risingRowTimer = 0f;
            riseBottomRow();
            if (gameOver) return;
        }

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
        if (!canControl()) return false;
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
        if (!canControl()) return false;
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
        if (!canControl()) return false;
        if (fits(active.type, active.rotation, active.x, active.y + 1)) {
            active.y += 1;
            score += 1;
            fallTimer = 0f;
            lockResets = 0;
            return true;
        }
        return false;
    }

    public void hardDrop() {
        if (!canControl()) return;
        int traveled = 0;
        while (fits(active.type, active.rotation, active.x, active.y + 1)) {
            active.y += 1;
            traveled++;
        }
        score += traveled * 3;
        lockPiece();
    }

    private void lockPiece() {
        int[][] shape = SHAPES[active.type][active.rotation];
        for (int[] cell : shape) {
            int boardX = active.x + cell[0];
            int boardY = active.y + cell[1];
            if (boardY < 0) {
                gameOver = true;
                spawnGameOverBurst();
                return;
            }
            board[boardY][boardX] = active.type + 1;
        }
        
        active = null; // Piece locked, clear active reference
        
        // Check for filled lines immediately upon lock
        int cleared = checkAndClearFilledLines();
        
        if (cleared > 0) {
            // Trigger cascade resolution ONLY when lines are cleared!
            isResolving = true;
            resolveTimer = 0.08f; // fast first check
        } else {
            // If no lines cleared, absolutely nothing happens, everything remains stable!
            comboSystem.processLinesCleared(0);
            isResolving = false;
            spawnPiece();
        }
    }

    private void spawnPiece() {
        active = next;
        active.x = 4;
        active.y = -2;
        active.rotation = 0;
        next = new Piece(drawFromBag(), 4, 0, 0);
        fallTimer = 0f;
        isGrounded = false;
        lockDelayTimer = 0f;
        lockResets = 0;
        if (!fits(active.type, active.rotation, active.x, active.y)) {
            gameOver = true;
            spawnGameOverBurst();
        }
    }

    private boolean applyGravityStep() {
        boolean changed = false;

        for (int c = 0; c < COLS; c++) {
            for (int r = ROWS - 1; r > 0; r--) {
                if (board[r][c] == 0 && board[r - 1][c] != 0) {
                    board[r][c] = board[r - 1][c];
                    board[r - 1][c] = 0;
                    changed = true;
                }
            }
        }

        return changed;
    }

    private int checkAndClearFilledLines() {
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
            return 0;
        }

        // Record line bursts and clear
        for (int rowIndex : cleared) {
            int[] colors = new int[COLS];
            System.arraycopy(board[rowIndex], 0, colors, 0, COLS);
            lineBursts.add(new LineBurst(rowIndex, colors, false));
            for (int col = 0; col < COLS; col++) {
                board[rowIndex][col] = 0;
            }
        }

        int count = cleared.size();
        linesCleared += count;
        
        // Feed into existing combo architecture
        int scoreToAdd = comboSystem.processLinesCleared(count);
        score += scoreToAdd * level;

        if (comboSystem.getComboCount() >= 2) {
            comboDisplayTimer = 1.0f; // display combo on HUD for 1 sec
        }

        // Slower level scale to prevent rapid speed spike
        level = (linesCleared / 12) + 1;

        return count;
    }

    private void riseBottomRow() {
        // 1. Check game over before rise
        for (int c = 0; c < COLS; c++) {
            if (board[0][c] != 0) {
                gameOver = true;
                spawnGameOverBurst();
                return;
            }
        }

        // 2. Shift everything up by 1 row
        for (int r = 0; r < ROWS - 1; r++) {
            System.arraycopy(board[r + 1], 0, board[r], 0, COLS);
        }

        // 3. Generate a new bottom row with 1 to 3 random holes
        int numHoles = random.nextInt(3) + 1; // 1 to 3 holes
        ArrayList<Integer> holeCols = new ArrayList<>();
        for (int i = 0; i < COLS; i++) {
            holeCols.add(i);
        }
        Collections.shuffle(holeCols, random);
        List<Integer> selectedHoles = holeCols.subList(0, numHoles);

        int[] newRow = new int[COLS];
        for (int c = 0; c < COLS; c++) {
            if (selectedHoles.contains(c)) {
                newRow[c] = 0;
            } else {
                newRow[c] = random.nextInt(7) + 1; // Block of color
            }
        }
        System.arraycopy(newRow, 0, board[ROWS - 1], 0, COLS);

        // 4. Shift active piece upwards as well
        if (active != null) {
            active.y -= 1;
        }
    }

    private void spawnGameOverBurst() {
        for (int r = 0; r < ROWS; r++) {
            boolean any = false;
            int[] colors = new int[COLS];
            for (int c = 0; c < COLS; c++) {
                colors[c] = board[r][c];
                any |= colors[c] != 0;
            }
            if (any) {
                lineBursts.add(new LineBurst(r, colors, true));
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
        return !paused && !gameOver && active != null && !isResolving;
    }

    public float getFallInterval() {
        float speed = 0.8f - (level - 1) * 0.05f;
        return Math.max(0.12f, speed);
    }

    public float getProgressToFall() {
        if (active == null) return 0f;
        return Math.min(1f, fallTimer / getFallInterval());
    }

    public float getRisingRowInterval() {
        return Math.max(5.0f, 14.0f - (level - 1) * 0.8f);
    }

    public float getRisingRowProgress() {
        return Math.min(1f, risingRowTimer / getRisingRowInterval());
    }

    public int getCell(int r, int c) {
        return board[r][c];
    }

    public Piece getActive() {
        return active;
    }

    public Piece getNext() {
        return next;
    }

    public List<int[]> getActiveCells() {
        ArrayList<int[]> cells = new ArrayList<>();
        if (active == null) return cells;
        int[][] shape = SHAPES[active.type][active.rotation];
        for (int[] cell : shape) {
            cells.add(new int[]{active.x + cell[0], active.y + cell[1]});
        }
        return cells;
    }

    public List<int[]> getGhostCells() {
        ArrayList<int[]> cells = new ArrayList<>();
        if (active == null) return cells;
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

    public List<LineBurst> getLineBursts() {
        return lineBursts;
    }

    public ComboSystem getComboSystem() {
        return comboSystem;
    }

    public float getComboDisplayTimer() {
        return comboDisplayTimer;
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        return level;
    }

    public int getLinesCleared() {
        return linesCleared;
    }

    public boolean isPaused() {
        return paused;
    }

    public void togglePause() {
        if (!gameOver) {
            paused = !paused;
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isResolving() {
        return isResolving;
    }

    public static int[][] getShapeCells(int type, int rotation) {
        return SHAPES[type][rotation];
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
