package com.brickspaceneo.modes.snake;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeEngine {
    public static final int COLS = 14;
    public static final int ROWS = 22;

    public static class Point {
        public int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Point) {
                Point p = (Point) obj;
                return p.x == x && p.y == y;
            }
            return false;
        }
    }

    private final List<Point> snake = new ArrayList<>();
    private Point food;
    private int directionX = 0;
    private int directionY = -1; // Default moving up
    private int nextDirectionX = 0;
    private int nextDirectionY = -1;
    
    private int score = 0;
    private int comboMultiplier = 1;
    private int level = 1;
    private int lives = 3;
    private int foodEatenCount = 0;
    private boolean gameOver = false;
    private boolean paused = false;
    
    private float moveTimer = 0f;
    private final Random random = new Random();

    public SnakeEngine() {
        resetGame();
    }

    public void resetGame() {
        snake.clear();
        int startX = COLS / 2;
        int startY = ROWS / 2;
        snake.add(new Point(startX, startY));
        snake.add(new Point(startX, startY + 1));
        snake.add(new Point(startX, startY + 2));
        
        directionX = 0;
        directionY = -1;
        nextDirectionX = 0;
        nextDirectionY = -1;
        
        spawnFood();
        score = 0;
        comboMultiplier = 1;
        level = 1;
        lives = 3;
        foodEatenCount = 0;
        gameOver = false;
        paused = false;
    }

    public void resetLife() {
        snake.clear();
        int startX = COLS / 2;
        int startY = ROWS / 2;
        snake.add(new Point(startX, startY));
        snake.add(new Point(startX, startY + 1));
        snake.add(new Point(startX, startY + 2));
        
        directionX = 0;
        directionY = -1;
        nextDirectionX = 0;
        nextDirectionY = -1;
        
        spawnFood();
        gameOver = false;
    }

    public void update(float delta) {
        if (gameOver || paused) return;

        moveTimer += delta;
        float speed = getSpeed();
        if (moveTimer >= speed) {
            moveTimer = 0f;
            move();
        }
    }

    private void move() {
        directionX = nextDirectionX;
        directionY = nextDirectionY;

        Point head = snake.get(0);
        int newX = head.x + directionX;
        int newY = head.y + directionY;

        // Collision with walls
        if (newX < 0 || newX >= COLS || newY < 0 || newY >= ROWS) {
            handleDeath();
            return;
        }

        Point newHead = new Point(newX, newY);

        // Collision with body
        for (int i = 0; i < snake.size() - 1; i++) {
            if (snake.get(i).equals(newHead)) {
                handleDeath();
                return;
            }
        }

        snake.add(0, newHead);

        if (newHead.equals(food)) {
            score += 10 * comboMultiplier;
            comboMultiplier++;
            foodEatenCount++;
            if (foodEatenCount >= 3) {
                level++;
                foodEatenCount = 0;
                if (level > 15) level = 1;
            }
            spawnFood();
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    private void handleDeath() {
        lives--;
        comboMultiplier = 1;
        if (lives <= 0) {
            gameOver = true;
        } else {
            resetLife();
        }
    }

    private void spawnFood() {
        while (true) {
            int x = random.nextInt(COLS);
            int y = random.nextInt(ROWS);
            Point p = new Point(x, y);
            boolean onSnake = false;
            for (Point sp : snake) {
                if (sp.equals(p)) {
                    onSnake = true;
                    break;
                }
            }
            if (!onSnake) {
                food = p;
                break;
            }
        }
    }

    public void setDirection(int dx, int dy) {
        // Prevent 180 degree turns
        if (dx != 0 && directionX != -dx) {
            nextDirectionX = dx;
            nextDirectionY = 0;
        } else if (dy != 0 && directionY != -dy) {
            nextDirectionX = 0;
            nextDirectionY = dy;
        }
    }

    private float getSpeed() {
        // Level 1: 0.3s, Level 15: ~0.05s
        return Math.max(0.05f, 0.3f - (level - 1) * 0.017f);
    }

    public List<Point> getSnake() { return snake; }
    public Point getFood() { return food; }
    public int getScore() { return score; }
    public int getCombo() { return comboMultiplier; }
    public int getLevel() { return level; }
    public int getLives() { return lives; }
    public boolean isGameOver() { return gameOver; }
    public boolean isPaused() { return paused; }
    public void togglePause() { paused = !paused; }
    public int getFoodEatenCount() { return foodEatenCount; }
}
