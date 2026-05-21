package com.brickspaceneo.modes.breaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BreakerEngine {
    public static final int COLS = 10;
    public static final int ROWS = 20;
    public static final int MAX_BALLS = 6;
    public static final int DANGER_ZONE_START_ROW = 18;

    private final Random random = new Random();
    private final int[][] board = new int[ROWS][COLS];
    private final int[][] brickHP = new int[ROWS][COLS];

    private float paddleX = 5.0f;
    private float paddleTargetX = 5.0f;
    private final float paddleWidth = 2.8f; // Constant paddle width as per feedback
    private final float paddleMoveSpeed = 10.0f; // Speed when moving via buttons
    private final float paddleY = 19.0f; // Positioned at row 19

    private final ArrayList<Ball> activeBalls = new ArrayList<>();
    private final ArrayList<Explosion> activeExplosions = new ArrayList<>();
    private final ArrayList<LineBurst> lineBursts = new ArrayList<>();

    private float ballSpeedMultiplier = 1.0f; // Adjusted manually via Up/Down buttons
    private float fallingRowTimer = 0.0f;
    private float survivalTimer = 0.0f;

    private int score = 0;
    private int level = 1;
    private int lives = 3;
    private boolean paused = false;
    private boolean gameOver = false;

    private int comboCount = 0;
    private float comboTimer = 0.0f;

    // Events to trigger audio cues in GameView
    private boolean triggerSoundBounce = false;
    private boolean triggerSoundDestroy = false;
    private boolean triggerSoundPowerup = false;
    private boolean triggerSoundGameOver = false;

    public BreakerEngine() {
        restart();
    }

    public void restart() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                board[r][c] = 0;
                brickHP[r][c] = 0;
            }
        }
        activeBalls.clear();
        activeExplosions.clear();
        lineBursts.clear();

        paddleX = 5.0f;
        paddleTargetX = 5.0f;
        ballSpeedMultiplier = 1.0f;
        fallingRowTimer = 0.0f;
        survivalTimer = 0.0f;

        score = 0;
        level = 1;
        lives = 3;
        paused = false;
        gameOver = false;
        comboCount = 0;
        comboTimer = 0.0f;

        clearSounds();

        // Spawn initial rows of blocks at the top
        for (int r = 0; r < 4; r++) {
            generateRowAtTop(r);
        }

        // Spawn first ball
        activeBalls.add(new Ball(paddleX, paddleY - 0.5f, 2.0f, -4.5f, 0.0f));
    }

    public void update(float deltaSeconds) {
        if (paused || gameOver) {
            decayEffects(deltaSeconds);
            return;
        }

        decayEffects(deltaSeconds);

        // Accumulate survival points
        survivalTimer += deltaSeconds;
        if (survivalTimer >= 1.0f) {
            survivalTimer -= 1.0f;
            score += 10 + 5 * activeBalls.size(); // Time bonus + active balls multiplier bonus
        }

        // Smoothly interpolate paddle position
        float halfW = paddleWidth / 2.0f;
        paddleTargetX = Math.max(halfW, Math.min(COLS - halfW, paddleTargetX));
        paddleX += (paddleTargetX - paddleX) * Math.min(1.0f, deltaSeconds * 14.0f);
        paddleX = Math.max(halfW, Math.min(COLS - halfW, paddleX));

        // Procedural falling rows update
        fallingRowTimer += deltaSeconds;
        float rowInterval = getFallingRowInterval();
        if (fallingRowTimer >= rowInterval) {
            fallingRowTimer = 0.0f;
            shiftRowsDown();
        }

        if (gameOver) return;

        // Physics sub-stepping to prevent tunneling at high speeds
        int subSteps = ballSpeedMultiplier > 1.6f ? 3 : 2;
        float subDelta = deltaSeconds / subSteps;

        for (int step = 0; step < subSteps; step++) {
            updateBallsPhysics(subDelta);
        }

        // Check if all balls are lost
        if (activeBalls.isEmpty() && !gameOver) {
            lives--;
            if (lives > 0) {
                // Respawn single ball
                activeBalls.add(new Ball(paddleX, paddleY - 0.5f, 2.0f, -4.5f, random.nextFloat() * 360f));
                ballSpeedMultiplier = 1.0f; // Reset speed modifier for safety
            } else {
                gameOver = true;
                triggerSoundGameOver = true;
                spawnGameOverBursts();
            }
        }
    }

    private void decayEffects(float deltaSeconds) {
        if (comboTimer > 0) {
            comboTimer -= deltaSeconds;
            if (comboTimer <= 0) {
                comboCount = 0;
            }
        }

        for (int i = activeExplosions.size() - 1; i >= 0; i--) {
            Explosion exp = activeExplosions.get(i);
            exp.age += deltaSeconds;
            if (exp.age >= exp.maxAge) {
                activeExplosions.remove(i);
            }
        }

        for (int i = lineBursts.size() - 1; i >= 0; i--) {
            LineBurst burst = lineBursts.get(i);
            burst.age += deltaSeconds;
            if (burst.age > 1.25f) {
                lineBursts.remove(i);
            }
        }
    }

    private void updateBallsPhysics(float subDelta) {
        float ballRadius = 0.22f;

        for (int i = activeBalls.size() - 1; i >= 0; i--) {
            Ball ball = activeBalls.get(i);

            // Move
            ball.x += ball.vx * ballSpeedMultiplier * subDelta;
            ball.y += ball.vy * ballSpeedMultiplier * subDelta;

            // Update trail
            ball.updateTrail();

            // Wall Collisions
            if (ball.x - ballRadius <= 0) {
                ball.vx = Math.abs(ball.vx);
                ball.x = ballRadius;
                triggerSoundBounce = true;
            } else if (ball.x + ballRadius >= COLS) {
                ball.vx = -Math.abs(ball.vx);
                ball.x = COLS - ballRadius;
                triggerSoundBounce = true;
            }

            if (ball.y - ballRadius <= 0) {
                ball.vy = Math.abs(ball.vy);
                ball.y = ballRadius;
                triggerSoundBounce = true;
            }

            // Bottom out check
            if (ball.y - ballRadius > ROWS) {
                activeBalls.remove(i);
                continue;
            }

            // Paddle Collision
            if (ball.vy > 0) {
                float halfPaddleWidth = paddleWidth / 2.0f;
                // Check if ball intersects paddle horizontally and vertically
                if (ball.y + ballRadius >= paddleY - 0.15f && ball.y - ballRadius <= paddleY + 0.15f) {
                    if (ball.x >= paddleX - halfPaddleWidth && ball.x <= paddleX + halfPaddleWidth) {
                        // Collision! Reflect based on hit location
                        float hitFactor = (ball.x - paddleX) / halfPaddleWidth; // [-1.0, 1.0]
                        hitFactor = Math.max(-0.9f, Math.min(0.9f, hitFactor));

                        float speed = (float) Math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy);
                        float maxAngleX = 0.82f;

                        ball.vx = hitFactor * maxAngleX * speed;
                        ball.vy = - (float) Math.sqrt(Math.max(0.2f * speed * speed, speed * speed - ball.vx * ball.vx));
                        ball.y = paddleY - 0.15f - ballRadius; // Push up out of paddle
                        
                        triggerSoundBounce = true;
                        continue;
                    }
                }
            }

            // Brick Collisions
            checkBrickCollisions(ball, ballRadius);
        }
    }

    private void checkBrickCollisions(Ball ball, float ballRadius) {
        int cellX = (int) Math.floor(ball.x);
        int cellY = (int) Math.floor(ball.y);

        float minCollisionDist = Float.MAX_VALUE;
        int hitCol = -1, hitRow = -1;
        float hitNx = 0, hitNy = 0;
        float hitClosestX = 0, hitClosestY = 0;

        // Scan neighboring cells
        for (int r = cellY - 1; r <= cellY + 1; r++) {
            for (int c = cellX - 1; c <= cellX + 1; c++) {
                if (c < 0 || c >= COLS || r < 0 || r >= ROWS) continue;
                if (board[r][c] == 0) continue;

                // Closest point on AABB to Circle
                float closestX = Math.max((float) c, Math.min(ball.x, (float) c + 1.0f));
                float closestY = Math.max((float) r, Math.min(ball.y, (float) r + 1.0f));

                float dx = ball.x - closestX;
                float dy = ball.y - closestY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist < ballRadius && dist < minCollisionDist) {
                    minCollisionDist = dist;
                    hitCol = c;
                    hitRow = r;
                    hitClosestX = closestX;
                    hitClosestY = closestY;

                    if (dist == 0) {
                        hitNx = 0;
                        hitNy = -1;
                    } else {
                        hitNx = dx / dist;
                        hitNy = dy / dist;
                    }
                }
            }
        }

        if (hitCol != -1) {
            // Reflect ball velocity
            float dot = ball.vx * hitNx + ball.vy * hitNy;
            if (dot < 0) {
                ball.vx = ball.vx - 2.0f * dot * hitNx;
                ball.vy = ball.vy - 2.0f * dot * hitNy;
            }

            // Move ball out of collision
            ball.x = hitClosestX + hitNx * ballRadius;
            ball.y = hitClosestY + hitNy * ballRadius;

            damageBrick(hitCol, hitRow, ball);
        }
    }

    private void damageBrick(int col, int row, Ball ball) {
        int type = board[row][col];
        if (type == 0) return;

        // Trigger combo buildup
        comboCount++;
        comboTimer = 1.6f;
        int multiplier = Math.min(5, 1 + comboCount / 3);

        if (type >= 1 && type <= 7) {
            // Standard Brick
            board[row][col] = 0;
            triggerSoundDestroy = true;
            spawnBrickBurst(col, row, type);
            score += (50 + type * 10) * multiplier;
        } else if (type == 8) {
            // Durable Brick
            brickHP[row][col]--;
            if (brickHP[row][col] <= 0) {
                board[row][col] = 0;
                triggerSoundDestroy = true;
                spawnBrickBurst(col, row, 8);
                score += 150 * multiplier;
            } else {
                triggerSoundBounce = true;
                score += 30 * multiplier;
            }
        } else if (type == 9) {
            // PLUS Block
            board[row][col] = 0;
            triggerSoundPowerup = true;
            spawnBrickBurst(col, row, 9);
            score += 200 * multiplier;

            if (activeBalls.size() < MAX_BALLS) {
                float speed = (float) Math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy);
                float angle = (float) (random.nextFloat() * Math.PI * 0.8f + Math.PI * 0.1f); // angled upwards
                float vx = (float) (Math.cos(angle) * speed);
                float vy = - (float) (Math.sin(angle) * speed);
                activeBalls.add(new Ball(ball.x, ball.y, vx, vy, random.nextFloat() * 360f));
            }
        } else if (type == 10) {
            // MINUS Block
            board[row][col] = 0;
            triggerSoundDestroy = true;
            score += 250 * multiplier;
            triggerExplosion(col, row, multiplier);
        }

        // Adjust level based on score milestones
        level = 1 + score / 5000;
    }

    private void triggerExplosion(int centerCol, int centerRow, int comboMult) {
        ArrayList<int[]> queue = new ArrayList<>();
        queue.add(new int[]{centerCol, centerRow});

        while (!queue.isEmpty()) {
            int[] pos = queue.remove(0);
            int col = pos[0];
            int row = pos[1];

            activeExplosions.add(new Explosion(col + 0.5f, row + 0.5f));

            // Clear a 3x3 area
            for (int r = row - 1; r <= row + 1; r++) {
                for (int c = col - 1; c <= col + 1; c++) {
                    if (c < 0 || c >= COLS || r < 0 || r >= ROWS) continue;

                    int val = board[r][c];
                    if (val != 0) {
                        board[r][c] = 0;
                        brickHP[r][c] = 0;
                        spawnBrickBurst(c, r, val);

                        // If neighboring cell was another MINUS block, enqueue chain reaction
                        if (val == 10) {
                            queue.add(new int[]{c, r});
                            score += 250 * comboMult;
                        } else if (val == 8) {
                            score += 150 * comboMult;
                        } else if (val == 9) {
                            score += 200 * comboMult;
                        } else {
                            score += (50 + val * 10) * comboMult;
                        }
                        comboCount++;
                    }
                }
            }
        }
    }

    private void shiftRowsDown() {
        // If any brick is shifted into the Danger Zone (row 18/19), it's game over!
        for (int c = 0; c < COLS; c++) {
            if (board[DANGER_ZONE_START_ROW - 1][c] != 0) {
                gameOver = true;
                triggerSoundGameOver = true;
                spawnGameOverBursts();
                return;
            }
        }

        // Shift rows down
        for (int r = ROWS - 1; r > 0; r--) {
            System.arraycopy(board[r - 1], 0, board[r], 0, COLS);
            System.arraycopy(brickHP[r - 1], 0, brickHP[r], 0, COLS);
        }

        // Generate a new row at the top
        generateRowAtTop(0);
    }

    private void generateRowAtTop(int r) {
        // Enforce at least 1-3 empty column gaps so the balls can pass
        int numGaps = random.nextInt(3) + 1;
        ArrayList<Integer> cols = new ArrayList<>();
        for (int i = 0; i < COLS; i++) cols.add(i);
        java.util.Collections.shuffle(cols, random);
        List<Integer> gaps = cols.subList(0, numGaps);

        for (int c = 0; c < COLS; c++) {
            if (gaps.contains(c)) {
                board[r][c] = 0;
                brickHP[r][c] = 0;
            } else {
                float roll = random.nextFloat();
                if (roll < 0.70f) {
                    board[r][c] = random.nextInt(7) + 1; // Standard brick colors 1..7
                } else if (roll < 0.85f) {
                    board[r][c] = 8; // Durable
                    brickHP[r][c] = random.nextInt(2) + 2; // 2 or 3 HP
                } else if (roll < 0.92f) {
                    board[r][c] = 9; // PLUS Block
                } else {
                    board[r][c] = 10; // MINUS Block
                }
            }
        }
    }

    private void spawnBrickBurst(int col, int row, int colorVal) {
        int[] colors = new int[COLS];
        for (int i = 0; i < COLS; i++) {
            colors[i] = i == col ? colorVal : 0;
        }
        lineBursts.add(new LineBurst(row, colors, false));
    }

    private void spawnGameOverBursts() {
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

    // Controls
    public void setTargetPaddleX(float touchX) {
        this.paddleTargetX = touchX;
    }

    public void movePaddleLeft(float delta) {
        paddleTargetX = Math.max(paddleWidth / 2f, paddleTargetX - paddleMoveSpeed * delta);
    }

    public void movePaddleRight(float delta) {
        paddleTargetX = Math.min(COLS - paddleWidth / 2f, paddleTargetX + paddleMoveSpeed * delta);
    }

    public void increaseSpeed() {
        ballSpeedMultiplier = Math.min(2.5f, ballSpeedMultiplier + 0.15f);
    }

    public void decreaseSpeed() {
        ballSpeedMultiplier = Math.max(0.5f, ballSpeedMultiplier - 0.15f);
    }

    // Getters
    public int getCell(int r, int c) {
        return board[r][c];
    }

    public int getBrickHP(int r, int c) {
        return brickHP[r][c];
    }

    public float getPaddleX() {
        return paddleX;
    }

    public float getPaddleWidth() {
        return paddleWidth;
    }

    public float getPaddleY() {
        return paddleY;
    }

    public List<Ball> getActiveBalls() {
        return activeBalls;
    }

    public List<Explosion> getActiveExplosions() {
        return activeExplosions;
    }

    public List<LineBurst> getLineBursts() {
        return lineBursts;
    }

    public float getBallSpeedMultiplier() {
        return ballSpeedMultiplier;
    }

    public float getFallingRowInterval() {
        // Spawns rows faster as level increases (level 1 is 14s, level 10 is 5s)
        return Math.max(3.0f, 15.0f - (level - 1) * 1.0f);
    }

    public float getFallingRowProgress() {
        return Math.min(1.0f, fallingRowTimer / getFallingRowInterval());
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        return level;
    }

    public int getLives() {
        return lives;
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

    public int getComboCount() {
        return comboCount;
    }

    public float getComboDisplayTimer() {
        return comboTimer;
    }

    // Audio cues getters
    public boolean checkBounceSound() {
        boolean trigger = triggerSoundBounce;
        triggerSoundBounce = false;
        return trigger;
    }

    public boolean checkDestroySound() {
        boolean trigger = triggerSoundDestroy;
        triggerSoundDestroy = false;
        return trigger;
    }

    public boolean checkPowerupSound() {
        boolean trigger = triggerSoundPowerup;
        triggerSoundPowerup = false;
        return trigger;
    }

    public boolean checkGameOverSound() {
        boolean trigger = triggerSoundGameOver;
        triggerSoundGameOver = false;
        return trigger;
    }

    private void clearSounds() {
        triggerSoundBounce = false;
        triggerSoundDestroy = false;
        triggerSoundPowerup = false;
        triggerSoundGameOver = false;
    }

    public static final class Ball {
        public float x, y;
        public float vx, vy;
        public float hueOffset;
        public final List<float[]> trail = new ArrayList<>();

        private Ball(float x, float y, float vx, float vy, float hueOffset) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.hueOffset = hueOffset;
        }

        private void updateTrail() {
            trail.add(0, new float[]{x, y});
            if (trail.size() > 8) {
                trail.remove(trail.size() - 1);
            }
        }
    }

    public static final class Explosion {
        public final float col, row;
        public float age;
        public final float maxAge = 0.4f;

        private Explosion(float col, float row) {
            this.col = col;
            this.row = row;
            this.age = 0f;
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
            this.age = 0f;
            this.emitted = false;
        }
    }
}
