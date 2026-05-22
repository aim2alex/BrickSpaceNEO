package com.brickspaceneo;

import com.brickspaceneo.common.ComboSystem;
import com.brickspaceneo.modes.tetra.TetraEngine;
import com.brickspaceneo.modes.snake.SnakeEngine;
import com.brickspaceneo.modes.columns.ColumnsEngine;
import com.brickspaceneo.modes.collapse.CollapseEngine;
import com.brickspaceneo.modes.breaker.BreakerEngine;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.app.Activity;
import android.os.Build;
import android.view.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends View {
    private static final int[] PIECE_COLORS = new int[]{
            0xFF57C7FF,
            0xFFFF8F52,
            0xFFFFD35A,
            0xFF92F07D,
            0xFFF77AD9,
            0xFF9D7BFF,
            0xFFFF6C6C
    };

    private static final int[] COSMIC_COLORS = new int[]{
            0xFF00E5FF, // Cyan
            0xFFF21EE0, // Neon Pink
            0xFFB293FE, // Soft Purple
            0xFF1A33E0, // Blue
            0xFFFFFFFF  // White
    };

    private final Random random = new Random(11L);

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockShadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint hudValuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint hudLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint buttonPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint overlayTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint helperPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private final RectF boardRect = new RectF();
    private final RectF boardGlowRect = new RectF();
    private final RectF mainScoreRect = new RectF();
    private final RectF nextRect = new RectF();
    private final RectF progressRect = new RectF();
    private final RectF rotateButton = new RectF();
    private final RectF leftButton = new RectF();
    private final RectF rightButton = new RectF();
    private final RectF dropButton = new RectF();
    private final RectF pauseButton = new RectF();
    private final RectF bestScoreRect = new RectF();

    private final RectF menuModalRect = new RectF();
    private final RectF continueBtnRect = new RectF();
    private final RectF restartBtnRect = new RectF();
    private final RectF homeBtnRect = new RectF();

    private final RectF startGameBtnRect = new RectF();
    private final RectF optionsBtnRect = new RectF();
    private final RectF exitBtnRect = new RectF();
    private final RectF resetScoresBtnRect = new RectF();
    
    private final RectF controlsToggleBtnRect = new RectF();
    private final RectF optionsMusicBtnRect = new RectF();
    private final RectF optionsSoundBtnRect = new RectF();
    private final RectF optionsGrayscaleBtnRect = new RectF();
    private final RectF optionsBackBtnActualRect = new RectF();
    private final RectF optionsBackBtnRect = new RectF();
    
    private final RectF gameOverRestartBtnRect = new RectF();
    private final RectF gameOverMenuBtnRect = new RectF();

    private final RectF bricksTileRect = new RectF();
    private final RectF snakeTileRect = new RectF();
    private final RectF breakerTileRect = new RectF();
    private final RectF gameSelectionBackBtnRect = new RectF();
    
    // Snake specific UI
    private final RectF snakeLivesPanelRect = new RectF();
    private final RectF snakeLevelBarRect = new RectF();

    private final Path blobPath = new Path();
    private final ArrayList<BackgroundOrb> orbs = new ArrayList<>();
    private final ArrayList<JellyParticle> particles = new ArrayList<>();
    private final ArrayList<FloatingShape> floatingShapes = new ArrayList<>();
    private final RectF particleBounds = new RectF();

    private long lastFrameNanos;
    private float boardShake;
    private float orbLinePull;
    private int insetLeft;
    private int insetTop;
    private int insetRight;
    private int insetBottom;
    private int prevScore;
    private int bestScore = 0;
    private int bestScoreSnake = 0;
    private int bestScoreColumns = 0;
    private int bestScoreCollapse = 0;
    private int bestScoreBreaker = 0;
    private int prevLines;
    private float columnsComboAnim = 0f;
    private float collapseComboAnim = 0f;
    private boolean prevGameOver;
    private boolean inMainMenu = true;
    private boolean inGameSelection = false;
    private boolean inOptions = false;
    
    public enum GameMode { BRICKS, SNAKE, COLUMNS, COLLAPSE, BREAKER }
    private GameMode currentGameMode = GameMode.BRICKS;
 
    private final TetraEngine engine = new TetraEngine();
    private final SnakeEngine snakeEngine = new SnakeEngine();
    private final ColumnsEngine columnsEngine = new ColumnsEngine();
    private final CollapseEngine collapseEngine = new CollapseEngine();
    private final BreakerEngine breakerEngine = new BreakerEngine();
    
    private boolean isLeftButtonPressed = false;
    private boolean isRightButtonPressed = false;
    
    // Selection rects
    private final RectF columnsTileRect = new RectF();
    private final RectF collapseTileRect = new RectF();

    private boolean useGestures = false;
    private float touchDownX;
    private float touchDownY;
    private float touchAnchorX;
    private boolean isSwipingHorizontally;
    private float mmStartY;
    private android.graphics.drawable.Drawable logoDrawable;

    private MediaPlayer bgMusic;
    private MediaPlayer bgMusicNext;
    private int musicDuration;
    private boolean crossfadeStarted = false;
    private int currentMusicResId;
    private SoundPool soundPool;
    private int soundRotate, soundDrop, soundClear, soundTetris, soundGameOver, soundHold;
    private int soundFood, soundLevelUp;
    private boolean musicEnabled = true;
    private boolean soundsEnabled = true;


    public GameView(Context context) {
        super(context);
        setFocusable(true);
        setClickable(true);

        SharedPreferences prefs = context.getSharedPreferences("BrickSpaceNeoPrefs", Context.MODE_PRIVATE);
        bestScore = prefs.getInt("BEST_SCORE", 0);
        bestScoreSnake = prefs.getInt("BEST_SCORE_SNAKE", 0);
        bestScoreColumns = prefs.getInt("BEST_SCORE_COLUMNS", 0);
        bestScoreCollapse = prefs.getInt("BEST_SCORE_COLLAPSE", 0);
        bestScoreBreaker = prefs.getInt("BEST_SCORE_BREAKER", 0);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setColor(0x18FFFFFF);

        blockStrokePaint.setStyle(Paint.Style.STROKE);
        blockStrokePaint.setColor(0x44FFFFFF);

        shadowPaint.setColor(0x66000000);

        titlePaint.setColor(Color.WHITE);
        titlePaint.setFakeBoldText(true);
        titlePaint.setTextAlign(Paint.Align.CENTER);

        hudValuePaint.setColor(Color.WHITE);
        hudValuePaint.setFakeBoldText(true);
        hudValuePaint.setTextAlign(Paint.Align.CENTER);

        hudLabelPaint.setColor(0xFFD8F4FF);
        hudLabelPaint.setTextAlign(Paint.Align.CENTER);
        hudLabelPaint.setFakeBoldText(true);

        buttonPaint.setColor(Color.WHITE);
        buttonPaint.setTextAlign(Paint.Align.CENTER);
        buttonPaint.setFakeBoldText(true);

        overlayTextPaint.setColor(Color.WHITE);
        overlayTextPaint.setTextAlign(Paint.Align.CENTER);
        overlayTextPaint.setFakeBoldText(true);

        helperPaint.setColor(0xD6E8F7FF);
        helperPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < 18; i++) {
            orbs.add(new BackgroundOrb(random.nextFloat(), random.nextFloat(), random.nextFloat()));
        }

        // Initialize 10 falling comets and 30 falling particles
        for (int i = 0; i < 10; i++) {
            FloatingShape shape = new FloatingShape();
            shape.type = 0; // Comet
            shape.x = random.nextFloat();
            shape.y = random.nextFloat();
            shape.vy = 0.12f + random.nextFloat() * 0.12f;
            shape.vx = -0.05f - random.nextFloat() * 0.07f;
            shape.rotation = random.nextFloat() * 360f;
            shape.spin = (random.nextFloat() - 0.5f) * 40f;
            shape.colorIndex = random.nextInt(COSMIC_COLORS.length);
            shape.sizeScale = 0.5f + random.nextFloat() * 0.7f;
            floatingShapes.add(shape);
        }
        for (int i = 0; i < 30; i++) {
            FloatingShape shape = new FloatingShape();
            shape.type = 1; // Particle
            shape.x = random.nextFloat();
            shape.y = random.nextFloat();
            shape.vy = 0.08f + random.nextFloat() * 0.10f;
            shape.vx = -0.03f - random.nextFloat() * 0.05f;
            shape.rotation = random.nextFloat() * 360f;
            shape.spin = (random.nextFloat() - 0.5f) * 40f;
            shape.colorIndex = random.nextInt(COSMIC_COLORS.length);
            shape.sizeScale = 0.4f + random.nextFloat() * 0.6f;
            floatingShapes.add(shape);
        }
        
        logoDrawable = context.getDrawable(R.drawable.brickspaceneo_logo);
        initAudio(context);
        updateSystemBars();
    }

    @SuppressWarnings("deprecation")
    private void updateSystemBars() {
        if (!(getContext() instanceof Activity)) return;
        Activity activity = (Activity) getContext();
        Window window = activity.getWindow();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowInsetsController controller = window.getDecorView().getWindowInsetsController();
            if (controller != null) {
                // Clear the light appearance flags so status/nav bar icons are light on dark
                controller.setSystemBarsAppearance(0, android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                controller.setSystemBarsAppearance(0, android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
            int flags = window.getDecorView().getSystemUiVisibility();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void initAudio(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("BrickSpaceNeoPrefs", Context.MODE_PRIVATE);
        musicEnabled = prefs.getBoolean("MUSIC_ENABLED", true);
        soundsEnabled = prefs.getBoolean("SOUNDS_ENABLED", true);

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build();

        soundRotate = soundPool.load(context, R.raw.rotate, 1);
        soundDrop = soundPool.load(context, R.raw.drop, 1);
        soundClear = soundPool.load(context, R.raw.line_clear, 1);
        soundTetris = soundPool.load(context, R.raw.tetris_clear, 1);
        soundGameOver = soundPool.load(context, R.raw.game_over, 1);
        soundHold = soundPool.load(context, R.raw.hold, 1);
        
        updateMusic();
    }

    private void updateMusic() {
        if (bgMusic != null) {
            bgMusic.stop();
            bgMusic.release();
            bgMusic = null;
        }
        if (bgMusicNext != null) {
            bgMusicNext.stop();
            bgMusicNext.release();
            bgMusicNext = null;
        }
        
        currentMusicResId = inMainMenu ? R.raw.menu_music : R.raw.game_music;
        bgMusic = MediaPlayer.create(getContext(), currentMusicResId);
        if (bgMusic != null) {
            bgMusic.setLooping(false); // We handle looping manually for crossfade
            musicDuration = bgMusic.getDuration();
            crossfadeStarted = false;
            if (musicEnabled && getWindowVisibility() == VISIBLE) bgMusic.start();
            
            // Pre-prepare next
            bgMusicNext = MediaPlayer.create(getContext(), currentMusicResId);
        }
    }

    private void updateAudioCrossfade() {
        if (!musicEnabled || bgMusic == null || bgMusicNext == null || !bgMusic.isPlaying()) return;
        
        int pos = bgMusic.getCurrentPosition();
        // Start crossfade 1 second before end
        if (!crossfadeStarted && pos > musicDuration - 1000) {
            crossfadeStarted = true;
            bgMusicNext.start();
            // Swap players for the next cycle
            MediaPlayer temp = bgMusic;
            bgMusic = bgMusicNext;
            bgMusicNext = temp;
            
            // Allow the old player to finish its tail
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (bgMusicNext != null) {
                    try {
                        bgMusicNext.stop();
                        bgMusicNext.prepare(); // Ready for next use
                    } catch (Exception e) {}
                }
                crossfadeStarted = false;
            }, 1000);
        }
    }


    private void playSound(int soundId) {
        if (soundsEnabled && soundPool != null) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        }
    }

    private void restartMusic() {
        updateMusic();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            if (musicEnabled && bgMusic != null && !bgMusic.isPlaying()) {
                bgMusic.start();
            }
        } else {
            if (bgMusic != null && bgMusic.isPlaying()) {
                bgMusic.pause();
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
            insetLeft = systemBars.left;
            insetTop = systemBars.top;
            insetRight = systemBars.right;
            insetBottom = systemBars.bottom;
        } else {
            insetLeft = insets.getSystemWindowInsetLeft();
            insetTop = insets.getSystemWindowInsetTop();
            insetRight = insets.getSystemWindowInsetRight();
            insetBottom = insets.getSystemWindowInsetBottom();
        }
        invalidate();
        return super.onApplyWindowInsets(insets);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 0.016f : Math.min(0.033f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;

        updateSimulation(delta);
        layoutScene();
        drawBackground(canvas, now * 0.000000001f);
        
        if (inMainMenu) {
            drawFloatingShapes(canvas);
            drawMainMenu(canvas);
        } else if (inGameSelection) {
            drawFloatingShapes(canvas);
            drawGameSelection(canvas);
        } else {
            if (currentGameMode == GameMode.BRICKS) {
                drawHud(canvas);
                canvas.save();
                float shakeX = (float) Math.sin(now * 0.0000000105f) * boardShake * dp(6);
                float shakeY = (float) Math.cos(now * 0.000000009f) * boardShake * dp(4);
                canvas.translate(shakeX, shakeY);
                drawBoard(canvas, now * 0.000000001f);
                canvas.restore();
                drawOverlay(canvas);
            } else if (currentGameMode == GameMode.SNAKE) {
                drawSnakeHud(canvas);
                canvas.save();
                float shakeX = (float) Math.sin(now * 0.0000000105f) * boardShake * dp(6);
                float shakeY = (float) Math.cos(now * 0.000000009f) * boardShake * dp(4);
                canvas.translate(shakeX, shakeY);
                drawSnakeBoard(canvas, now * 0.000000001f);
                canvas.restore();
                drawSnakeOverlay(canvas);
            } else if (currentGameMode == GameMode.COLUMNS) {
                drawColumnsHud(canvas);
                canvas.save();
                float shakeX = (float) Math.sin(now * 0.0000000105f) * boardShake * dp(6);
                float shakeY = (float) Math.cos(now * 0.000000009f) * boardShake * dp(4);
                canvas.translate(shakeX, shakeY);
                drawColumnsBoard(canvas, now * 0.000000001f);
                canvas.restore();
                drawColumnsOverlay(canvas);
            } else if (currentGameMode == GameMode.COLLAPSE) {
                drawCollapseHud(canvas);
                canvas.save();
                float shakeX = (float) Math.sin(now * 0.0000000105f) * boardShake * dp(6);
                float shakeY = (float) Math.cos(now * 0.000000009f) * boardShake * dp(4);
                canvas.translate(shakeX, shakeY);
                drawCollapseBoard(canvas, now * 0.000000001f);
                canvas.restore();
                drawCollapseOverlay(canvas);
            } else {
                drawBreakerHud(canvas);
                canvas.save();
                float shakeX = (float) Math.sin(now * 0.0000000105f) * boardShake * dp(6);
                float shakeY = (float) Math.cos(now * 0.000000009f) * boardShake * dp(4);
                canvas.translate(shakeX, shakeY);
                drawBreakerBoard(canvas, now * 0.000000001f);
                canvas.restore();
                drawBreakerOverlay(canvas);
            }
        }
        postInvalidateOnAnimation();
    }

    private void updateSimulation(float delta) {
        updateAudioCrossfade();
        if (inMainMenu || inGameSelection) {
            updateFloatingShapes(delta);
            return;
        }

        if (currentGameMode == GameMode.BRICKS) {
            engine.update(delta);

            if (engine.getScore() != prevScore) {
                emitBurst(boardRect.centerX(), boardRect.top + boardRect.height() * 0.18f, 10, 1f);
                prevScore = engine.getScore();
                if (prevScore > bestScore) {
                    bestScore = prevScore;
                    getContext().getSharedPreferences("BrickSpaceNeoPrefs", Context.MODE_PRIVATE)
                            .edit().putInt("BEST_SCORE", bestScore).apply();
                }
            }
            if (engine.getLinesCleared() != prevLines) {
                boardShake = 1f;
                triggerLinePull(engine.getLinesCleared() - prevLines);
                prevLines = engine.getLinesCleared();
            }
            if (engine.isGameOver() && !prevGameOver) {
                emitBurst(boardRect.centerX(), boardRect.centerY(), 36, 1.8f);
                playSound(soundGameOver);
            }
            prevGameOver = engine.isGameOver();

            List<TetraEngine.LineBurst> bursts = engine.getLineBursts();
            for (int i = 0; i < bursts.size(); i++) {
                TetraEngine.LineBurst burst = bursts.get(i);
                if (!burst.catastrophic && !burst.emitted) {
                    float cell = boardRect.width() / TetraEngine.COLS;
                    emitLineBurst(burst.row, burst.colors, cell);
                    burst.emitted = true;
                    if (engine.getComboSystem().getComboCount() >= 4) {
                        playSound(soundTetris);
                    } else {
                        playSound(soundClear);
                    }
                }
            }
        } else if (currentGameMode == GameMode.SNAKE) {
            // Snake Mode Simulation
            int oldLives = snakeEngine.getLives();
            int oldScore = snakeEngine.getScore();
            int oldLevel = snakeEngine.getLevel();
            
            snakeEngine.update(delta);
            
            if (snakeEngine.getScore() != oldScore) {
                playSound(soundClear); // Food pickup
                emitBurst(boardRect.left + snakeEngine.getFood().x * (boardRect.width() / SnakeEngine.COLS),
                          boardRect.top + snakeEngine.getFood().y * (boardRect.height() / SnakeEngine.ROWS), 8, 0.8f);
                if (snakeEngine.getScore() > bestScoreSnake) {
                    bestScoreSnake = snakeEngine.getScore();
                    getContext().getSharedPreferences("BrickSpaceNeoPrefs", Context.MODE_PRIVATE)
                            .edit().putInt("BEST_SCORE_SNAKE", bestScoreSnake).apply();
                }
            }
            if (snakeEngine.getLives() < oldLives) {
                boardShake = 0.8f;
                playSound(soundDrop); // Death
            }
            if (snakeEngine.getLevel() > oldLevel) {
                playSound(soundTetris); // Level Up
                emitBurst(boardRect.centerX(), boardRect.centerY(), 24, 1.5f);
            }
            if (snakeEngine.isGameOver() && !prevGameOver) {
                playSound(soundGameOver);
            }
            prevGameOver = snakeEngine.isGameOver();
        } else if (currentGameMode == GameMode.COLUMNS) {
            // Columns Mode Simulation
            int oldScore = columnsEngine.getScore();
            int oldLevel = columnsEngine.getLevel();
            
            columnsEngine.update(delta);
            
            if (columnsEngine.getScore() != oldScore) {
                if (columnsEngine.getScore() > bestScoreColumns) {
                    bestScoreColumns = columnsEngine.getScore();
                    getContext().getSharedPreferences("BrickSpaceNeoPrefs", Context.MODE_PRIVATE)
                            .edit().putInt("BEST_SCORE_COLUMNS", bestScoreColumns).apply();
                }
            }
            if (columnsEngine.getLevel() > oldLevel) {
                playSound(soundTetris);
                emitBurst(boardRect.centerX(), boardRect.centerY(), 24, 1.5f);
            }
            if (columnsEngine.isGameOver() && !prevGameOver) {
                playSound(soundGameOver);
                emitBurst(boardRect.centerX(), boardRect.centerY(), 32, 1.6f);
            }
            prevGameOver = columnsEngine.isGameOver();

            List<ColumnsEngine.MatchedCluster> matchedVisuals = columnsEngine.getAndClearVisuals();
            if (!matchedVisuals.isEmpty()) {
                float cellW = boardRect.width() / ColumnsEngine.COLS;
                float cellH = boardRect.height() / ColumnsEngine.ROWS;
                for (ColumnsEngine.MatchedCluster cluster : matchedVisuals) {
                    int colorIdx = (cluster.color - 1);
                    for (int[] pos : cluster.positions) {
                        float cx = boardRect.left + pos[1] * cellW + cellW / 2;
                        float cy = boardRect.top + pos[0] * cellH + cellH / 2;
                        // Increased count (16) and intensity (1.1f) for better "splashes"
                        emitBurst(cx, cy, 16, 1.1f, colorIdx);
                    }
                    // Reduced shake intensity
                    float comboShake = 0.2f + columnsEngine.getCombo() * 0.08f;
                    boardShake = Math.max(boardShake, comboShake);
                    
                    // Background ripple effect like in Bricks
                    triggerLinePull(cluster.positions.size() / 4 + 1);
                    
                    if (columnsEngine.getCombo() > 3) {
                        playSound(soundLevelUp);
                    } else {
                        playSound(soundClear);
                    }
                }
            }
            
            // Animate combo display (0 = center score, 1 = shifted score + combo label)
            float targetAnim = columnsEngine.getCombo() > 1 ? 1f : 0f;
            columnsComboAnim += (targetAnim - columnsComboAnim) * Math.min(1f, delta * 8f);
        } else if (currentGameMode == GameMode.COLLAPSE) {
            updateCollapseSimulation(delta);
        } else {
            updateBreakerSimulation(delta);
        }

        boardShake = Math.max(0f, boardShake - delta * 2.0f);
        orbLinePull = Math.max(0f, orbLinePull - delta * 1.8f);
        for (int i = 0; i < orbs.size(); i++) {
            BackgroundOrb orb = orbs.get(i);
            orb.pullVelocity += delta * dp(140) * orbLinePull;
            orb.pullOffset += orb.pullVelocity * delta;
            orb.pullVelocity *= (1f - Math.min(0.5f, delta * 2.6f));
            orb.pullOffset *= (1f - Math.min(0.35f, delta * 1.9f));
        }
        particleBounds.set(dp(8), dp(8), getWidth() - dp(8), getHeight() - dp(8));
        for (int i = particles.size() - 1; i >= 0; i--) {
            JellyParticle particle = particles.get(i);
            particle.age += delta;
            particle.x += particle.vx * delta;
            particle.y += particle.vy * delta;
            particle.vy += particle.gravity * delta;
            particle.vx *= (1f - particle.drag * delta);
            particle.vy *= (1f - particle.drag * delta);
            particle.rotation += particle.spin * delta;

            if (particle.y > particleBounds.bottom - particle.size) {
                particle.y = particleBounds.bottom - particle.size;
                particle.vy = -particle.vy * particle.bounce;
                particle.vx *= (1f - particle.floorFriction);
            }

            if (particle.age > particle.life || (particle.age > particle.life * 0.5f
                    && particle.y >= particleBounds.bottom - particle.size - dp(2)
                    && Math.abs(particle.vy) < dp(24) && Math.abs(particle.vx) < dp(12))) {
                particles.remove(i);
            }
        }
        updateFloatingShapes(delta);
    }

    private void updateCollapseSimulation(float delta) {
        int oldScore = collapseEngine.getScore();
        collapseEngine.update(delta);

        if (collapseEngine.getScore() != oldScore) {
            if (collapseEngine.getScore() > bestScoreCollapse) {
                bestScoreCollapse = collapseEngine.getScore();
                getContext().getSharedPreferences("BrickSpaceNeoPrefs", Context.MODE_PRIVATE)
                        .edit().putInt("BEST_SCORE_COLLAPSE", bestScoreCollapse).apply();
            }
        }

        if (collapseEngine.isGameOver() && !prevGameOver) {
            playSound(soundGameOver);
            emitBurst(boardRect.centerX(), boardRect.centerY(), 32, 1.6f);
        }
        prevGameOver = collapseEngine.isGameOver();

        List<CollapseEngine.LineBurst> bursts = collapseEngine.getLineBursts();
        for (int i = 0; i < bursts.size(); i++) {
            CollapseEngine.LineBurst burst = bursts.get(i);
            if (!burst.catastrophic && !burst.emitted) {
                float cell = boardRect.width() / CollapseEngine.COLS;
                emitLineBurst(burst.row, burst.colors, cell);
                burst.emitted = true;
                
                float comboShake = 0.25f + collapseEngine.getComboSystem().getComboCount() * 0.15f;
                boardShake = Math.max(boardShake, comboShake);
                
                triggerLinePull(2);

                if (collapseEngine.getComboSystem().getComboCount() >= 4) {
                    playSound(soundTetris);
                } else if (collapseEngine.getComboSystem().getComboCount() >= 2) {
                    playSound(soundLevelUp);
                } else {
                    playSound(soundClear);
                }
            }
        }

        float targetAnim = (collapseEngine.getComboSystem().getComboCount() >= 2 && collapseEngine.getComboDisplayTimer() > 0) ? 1f : 0f;
        collapseComboAnim += (targetAnim - collapseComboAnim) * Math.min(1f, delta * 8f);
    }

    private void updateFloatingShapes(float delta) {
        for (FloatingShape shape : floatingShapes) {
            shape.x += shape.vx * delta;
            shape.y += shape.vy * delta;
            shape.rotation += shape.spin * delta;
            if (shape.y > 1.1f) {
                shape.y = -0.1f;
                shape.x = random.nextFloat() * 1.2f;
            }
            if (shape.x < -0.1f) {
                shape.x = 1.1f;
                shape.y = random.nextFloat() * 1.2f;
            }
        }
    }

    private void layoutScene() {
        float width = getWidth();
        float height = getHeight();
        float horizontalInset = Math.max(insetLeft, insetRight);
        float sideMargin = Math.max(dp(16), horizontalInset + dp(10));
        float safeTop = Math.max(dp(20) + insetTop, height * 0.10f);
        float safeBottom = Math.max(dp(28) + insetBottom, height * 0.10f);

        float usableLeft = sideMargin;
        float usableRight = width - sideMargin;
        float usableWidth = usableRight - usableLeft;
        float top = safeTop;
        float nextPanelHeight = dp(90);
        float hudHeight = dp(54);
        float mainHudHeight = dp(84);
        float hudGap = dp(12);
        float boardGapTop = dp(14);
        float boardGapBottom = dp(14);
        float panelGap = dp(12);
        float halfGap = dp(8);

        float controlsTop = height - safeBottom - dp(74);
        float infoTop = controlsTop - panelGap - nextPanelHeight;

        float topButtonSize = hudHeight;
        pauseButton.set(usableLeft, top + (mainHudHeight - hudHeight) / 2f, usableLeft + topButtonSize, top + (mainHudHeight + hudHeight) / 2f);
        
        float centerWidth = usableWidth * 0.4f;
        mainScoreRect.set(usableLeft + (usableWidth - centerWidth) / 2f, top, usableLeft + (usableWidth + centerWidth) / 2f, top + mainHudHeight);

        bestScoreRect.set(mainScoreRect.right + dp(4), top + (mainHudHeight - hudHeight) / 2f, usableRight, top + (mainHudHeight + hudHeight) / 2f);

        nextRect.set(usableLeft, infoTop, usableLeft + usableWidth * 0.34f, infoTop + nextPanelHeight);
        progressRect.set(nextRect.right + dp(10), infoTop, usableRight, infoTop + nextPanelHeight);

        float boardWidth = usableWidth;
        float boardHeight = boardWidth * TetraEngine.ROWS / TetraEngine.COLS;
        float boardTop = mainScoreRect.bottom + hudGap;
        float boardBottomLimit = nextRect.top - boardGapBottom;
        float maxBoardHeight = Math.max(dp(280), boardBottomLimit - boardTop);
        if (boardHeight > maxBoardHeight) {
            boardHeight = maxBoardHeight;
            boardWidth = boardHeight * TetraEngine.COLS / TetraEngine.ROWS;
        }
        boardTop = Math.max(mainScoreRect.bottom + boardGapTop, boardBottomLimit - boardHeight);
        float boardLeft = (width - boardWidth) * 0.5f;
        boardRect.set(boardLeft, boardTop, boardLeft + boardWidth, boardTop + boardHeight);
        boardGlowRect.set(boardRect.left - dp(8), boardRect.top - dp(8), boardRect.right + dp(8), boardRect.bottom + dp(8));

        float btnGap = dp(14);
        float controlBtnSize = (usableWidth - btnGap * 3f) / 4f;
        
        leftButton.set(usableLeft, controlsTop, usableLeft + controlBtnSize, controlsTop + controlBtnSize);
        rotateButton.set(leftButton.right + btnGap, controlsTop, leftButton.right + btnGap + controlBtnSize, controlsTop + controlBtnSize);
        rightButton.set(rotateButton.right + btnGap, controlsTop, rotateButton.right + btnGap + controlBtnSize, controlsTop + controlBtnSize);
        dropButton.set(rightButton.right + btnGap, controlsTop, rightButton.right + btnGap + controlBtnSize, controlsTop + controlBtnSize);

        // Adjust board height for Snake/Columns if needed
        if (currentGameMode == GameMode.SNAKE) {
            float availableBoardHeight = infoTop - boardTop - boardGapBottom;
            if (boardHeight > availableBoardHeight) {
                boardHeight = availableBoardHeight;
                boardWidth = boardHeight * SnakeEngine.COLS / SnakeEngine.ROWS;
                boardLeft = (width - boardWidth) * 0.5f;
                boardRect.set(boardLeft, boardTop, boardLeft + boardWidth, boardTop + boardHeight);
            }
        } else if (currentGameMode == GameMode.COLUMNS) {
            float availableBoardHeight = infoTop - boardTop - boardGapBottom;
            boardHeight = availableBoardHeight;
            boardWidth = boardHeight * ColumnsEngine.COLS / ColumnsEngine.ROWS;
            boardLeft = (width - boardWidth) * 0.5f;
            boardRect.set(boardLeft, boardTop, boardLeft + boardWidth, boardTop + boardHeight);
        }

        float modalWidth = usableWidth * 0.8f;
        float modalHeight = dp(280);
        float modalLeft = (width - modalWidth) / 2f;
        float modalTop = (height - modalHeight) / 2f;
        menuModalRect.set(modalLeft, modalTop, modalLeft + modalWidth, modalTop + modalHeight);
        
        float modalBtnWidth = modalWidth * 0.8f;
        float modalBtnHeight = dp(54);
        float modalBtnLeft = modalLeft + (modalWidth - modalBtnWidth) / 2f;
        float btnSpacing = dp(16);
        float startY = modalTop + dp(70);
        
        continueBtnRect.set(modalBtnLeft, startY, modalBtnLeft + modalBtnWidth, startY + modalBtnHeight);
        restartBtnRect.set(modalBtnLeft, startY + modalBtnHeight + btnSpacing, modalBtnLeft + modalBtnWidth, startY + modalBtnHeight * 2 + btnSpacing);
        homeBtnRect.set(modalBtnLeft, startY + (modalBtnHeight + btnSpacing) * 2, modalBtnLeft + modalBtnWidth, startY + modalBtnHeight * 3 + btnSpacing * 2);

        float mmBtnWidth = usableWidth * 0.7f;
        float mmBtnHeight = dp(60);
        float mmBtnLeft = (width - mmBtnWidth) / 2f;
        mmStartY = height * 0.48f + dp(45); // Moved main menu buttons lower by 45px
        float mmSpacing = dp(20);
        
        startGameBtnRect.set(mmBtnLeft, mmStartY, mmBtnLeft + mmBtnWidth, mmStartY + mmBtnHeight);
        optionsBtnRect.set(mmBtnLeft, mmStartY + mmBtnHeight + mmSpacing, mmBtnLeft + mmBtnWidth, mmStartY + mmBtnHeight * 2 + mmSpacing);
        exitBtnRect.set(mmBtnLeft, mmStartY + (mmBtnHeight + mmSpacing) * 2, mmBtnLeft + mmBtnWidth, mmStartY + mmBtnHeight * 3 + mmSpacing * 2);
        
        float resetBtnHeight = dp(48);
        float resetBtnWidth = usableWidth * 0.75f;
        float resetBtnLeft = (width - resetBtnWidth) / 2f;
        float resetBtnBottom = height - insetBottom - dp(40);
        resetScoresBtnRect.set(resetBtnLeft, resetBtnBottom - resetBtnHeight, resetBtnLeft + resetBtnWidth, resetBtnBottom);

        // Game Selection Layout
        float tileWidth = (usableWidth - dp(16)) / 2f;
        float tileHeight = dp(110);
        float tileTop = height * 0.24f;
        bricksTileRect.set(usableLeft, tileTop, usableRight, tileTop + tileHeight);
        
        snakeTileRect.set(usableLeft, bricksTileRect.bottom + dp(12), usableLeft + tileWidth, bricksTileRect.bottom + dp(12) + tileHeight);
        columnsTileRect.set(snakeTileRect.right + dp(16), bricksTileRect.bottom + dp(12), snakeTileRect.right + dp(16) + tileWidth, bricksTileRect.bottom + dp(12) + tileHeight);
        
        collapseTileRect.set(usableLeft, columnsTileRect.bottom + dp(12), usableLeft + tileWidth, columnsTileRect.bottom + dp(12) + tileHeight);
        breakerTileRect.set(collapseTileRect.right + dp(16), columnsTileRect.bottom + dp(12), collapseTileRect.right + dp(16) + tileWidth, columnsTileRect.bottom + dp(12) + tileHeight);
        
        gameSelectionBackBtnRect.set(mmBtnLeft, breakerTileRect.bottom + dp(22), mmBtnLeft + mmBtnWidth, breakerTileRect.bottom + dp(22) + mmBtnHeight);

        // Snake Specific Layout (Matching Screenshot)
        snakeLivesPanelRect.set(usableLeft, infoTop, usableLeft + usableWidth * 0.32f, infoTop + nextPanelHeight);
        snakeLevelBarRect.set(snakeLivesPanelRect.right + dp(12), infoTop, usableRight, infoTop + nextPanelHeight);

        controlsToggleBtnRect.set(mmBtnLeft, mmStartY, mmBtnLeft + mmBtnWidth, mmStartY + mmBtnHeight);
        optionsBackBtnRect.set(mmBtnLeft, mmStartY + mmBtnHeight + mmSpacing, mmBtnLeft + mmBtnWidth, mmStartY + mmBtnHeight * 2 + mmSpacing);

        float goBtnWidth = usableWidth * 0.6f;
        float goBtnHeight = dp(54);
        float goBtnLeft = boardLeft + (boardWidth - goBtnWidth) / 2f;
        float goStartY = boardRect.centerY() + dp(40);
        
        gameOverRestartBtnRect.set(goBtnLeft, goStartY, goBtnLeft + goBtnWidth, goStartY + goBtnHeight);
        gameOverMenuBtnRect.set(goBtnLeft, goStartY + goBtnHeight + dp(14), goBtnLeft + goBtnWidth, goStartY + goBtnHeight * 2 + dp(14));

        titlePaint.setTextSize(dp(24));
        hudValuePaint.setTextSize(dp(20));
        hudLabelPaint.setTextSize(dp(11));
        buttonPaint.setTextSize(dp(24));
        overlayTextPaint.setTextSize(dp(28));
        helperPaint.setTextSize(dp(12));
        
        int headerColor = 0xFFFFFFFF;
        titlePaint.setColor(headerColor);
        hudValuePaint.setColor(headerColor);
        hudLabelPaint.setColor(0xFFD8F4FF);
        buttonPaint.setColor(headerColor);
        overlayTextPaint.setColor(headerColor);
        helperPaint.setColor(0xFFB8E1FF);

        gridPaint.setStrokeWidth(dp(1));
        blockStrokePaint.setStrokeWidth(dp(1.3f));
    }

    private void drawBackground(Canvas canvas, float time) {
        if (inMainMenu || inGameSelection) {
            // Main menu background - vertical gradient: top #020109, bottom #2a1b50
            int[] bgColors = new int[]{0xFF020109, 0xFF2A1B50};
            backgroundPaint.setShader(new LinearGradient(
                    0, 0, 0, getHeight(),
                    bgColors,
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
            return;
        }

        // Black (#000000) at top to Dark Purple (#0F033E) at bottom (Default gameplay background)
        int[] bgColors = new int[]{0xFF000000, 0xFF040114, 0xFF080228, 0xFF0F033E};
        backgroundPaint.setShader(new LinearGradient(
                0, 0, 0, getHeight(),
                bgColors,
                new float[]{0f, 0.35f, 0.7f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        glowPaint.setShader(new RadialGradient(
                getWidth() * 0.2f, getHeight() * 0.18f, getWidth() * 0.44f,
                0x3D7F00FF, 0x00000000, Shader.TileMode.CLAMP // Purple neon glow
        ));
        canvas.drawCircle(getWidth() * 0.2f, getHeight() * 0.18f, getWidth() * 0.44f, glowPaint);

        glowPaint.setShader(new RadialGradient(
                getWidth() * 0.85f, getHeight() * 0.16f, getWidth() * 0.26f,
                0x2AFF00AA, 0x00000000, Shader.TileMode.CLAMP // Magenta neon glow
        ));
        canvas.drawCircle(getWidth() * 0.85f, getHeight() * 0.16f, getWidth() * 0.26f, glowPaint);
        glowPaint.setShader(null);

        for (BackgroundOrb orb : orbs) {
            float x = orb.x * getWidth();
            float y = orb.y * getHeight();
            x += (float) Math.sin(time * (0.95f + orb.speed * 0.55f) + orb.phase) * dp(8);
            y += (float) Math.cos(time * (0.8f + orb.speed) + orb.phase) * dp(12);
            y += orb.pullOffset + dp(36) * orbLinePull;
            float radius = dp(3 + 7 * orb.speed) * (1f + orbLinePull * 0.12f);
            int color = Color.argb((int) (40 + orb.speed * 55 + orbLinePull * 18), 190, 244, 255);
            glowPaint.setColor(color);
            canvas.drawCircle(x, y, radius, glowPaint);
        }

        drawBlob(canvas, getWidth() * 0.08f, getHeight() * 0.76f, dp(110), 0x1A00F0FF, time * 0.7f); // Neon cyan blob
        drawBlob(canvas, getWidth() * 0.88f, getHeight() * 0.72f, dp(92), 0x1D9D7BFF, time * 0.9f + 1.4f); // Neon purple blob
    }

    private void drawHud(Canvas canvas) {
        drawButton(canvas, pauseButton, "", 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, bestScoreRect, 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, mainScoreRect, 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, nextRect, 0xC81C3045, 0x66FFE77B);
        drawGlassCard(canvas, progressRect, 0xC81A2940, 0x66FFFFFF);

        float cx = pauseButton.centerX();
        float cy = pauseButton.centerY();
        float pw = dp(4);
        float ph = dp(16);
        float pgap = dp(3);
        buttonPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(cx - pgap - pw, cy - ph/2f, cx - pgap, cy + ph/2f, dp(1), dp(1), buttonPaint);
        canvas.drawRoundRect(cx + pgap, cy - ph/2f, cx + pgap + pw, cy + ph/2f, dp(1), dp(1), buttonPaint);

        buttonPaint.setTextSize(dp(20));
        canvas.drawText("🏆", bestScoreRect.left + dp(22), bestScoreRect.centerY() - (buttonPaint.descent() + buttonPaint.ascent()) / 2, buttonPaint);
        
        hudLabelPaint.setTextAlign(Paint.Align.LEFT);
        hudValuePaint.setTextAlign(Paint.Align.LEFT);
        
        hudLabelPaint.setTextSize(dp(11));
        canvas.drawText("BEST", bestScoreRect.left + dp(44), bestScoreRect.top + dp(20), hudLabelPaint);
        drawAutoScaledText(canvas, String.valueOf(bestScore), bestScoreRect.left + dp(44), bestScoreRect.centerY() + dp(12), bestScoreRect.width() - dp(52), hudValuePaint, dp(18));
        
        hudLabelPaint.setTextAlign(Paint.Align.CENTER);
        hudValuePaint.setTextAlign(Paint.Align.CENTER);
        
        float slide = 0f;
        boolean comboActive = engine.getComboSystem().getComboCount() >= 2 && engine.getComboDisplayTimer() > 0;
        if (comboActive) {
            slide = Math.min(1f, engine.getComboDisplayTimer() / 0.3f);
        }

        float scoreLabelY = mainScoreRect.top + dp(30) - slide * dp(8);
        float scoreValueY = mainScoreRect.top + dp(60) - slide * dp(12);

        canvas.drawText("SCORE", mainScoreRect.centerX(), scoreLabelY, hudLabelPaint);
        drawAutoScaledText(canvas, String.valueOf(engine.getScore()), mainScoreRect.centerX(), scoreValueY, mainScoreRect.width() - dp(20), hudValuePaint, dp(32));
        hudValuePaint.setTextSize(dp(20));

        if (comboActive) {
            hudLabelPaint.setAlpha((int)(255 * slide));
            hudLabelPaint.setColor(0xFFFFD35A);
            canvas.drawText("x" + engine.getComboSystem().getCurrentMultiplier() + " COMBO", mainScoreRect.centerX(), mainScoreRect.top + dp(74), hudLabelPaint);
            hudLabelPaint.setColor(0xFFD8F4FF);
            hudLabelPaint.setAlpha(255);
        }

        canvas.drawText("NEXT", nextRect.centerX(), nextRect.top + dp(18), hudLabelPaint);
        drawNextPiece(canvas);

        canvas.drawText("SPEED FLOW", progressRect.centerX(), progressRect.top + dp(18), hudLabelPaint);
        drawProgress(canvas);
    }

    private void drawBoard(Canvas canvas, float time) {
        glowPaint.setShader(new RadialGradient(
                boardRect.centerX(), boardRect.top + boardRect.height() * 0.28f, boardRect.width() * 0.7f,
                0x3047C5FF, 0x00000000, Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(boardGlowRect, dp(28), dp(28), glowPaint);
        glowPaint.setShader(null);

        drawPlayfieldBackgroundAndStroke(canvas, boardRect);

        float cell = boardRect.width() / TetraEngine.COLS;
        float rowHeight = boardRect.height() / TetraEngine.ROWS;

        gridPaint.setColor(0x18FFFFFF);
        blockStrokePaint.setColor(0x44FFFFFF);
        for (int row = 0; row < TetraEngine.ROWS; row++) {
            for (int col = 0; col < TetraEngine.COLS; col++) {
                RectF rect = cellRect(col, row, cell, rowHeight);
                canvas.drawRoundRect(rect, dp(10), dp(10), gridPaint);
            }
        }

        List<int[]> ghost = engine.getGhostCells();
        TetraEngine.Piece activePiece = engine.getActive();
        int ghostColorIdx = (activePiece != null) ? activePiece.type : 0;
        for (int[] cellPos : ghost) {
            if (cellPos[1] >= 0) {
                drawGhostCell(canvas, cellPos[0], cellPos[1], ghostColorIdx, cell, rowHeight);
            }
        }

        for (int row = 0; row < TetraEngine.ROWS; row++) {
            for (int col = 0; col < TetraEngine.COLS; col++) {
                int value = engine.getCell(row, col);
                if (value != 0) {
                    drawJellyCell(canvas, col, row, value - 1, cell, rowHeight,
                            0.05f * (float) Math.sin(time * 2.2f + row * 0.4f + col * 0.7f),
                            0.08f * (float) Math.cos(time * 2.8f + col * 0.3f), false, false);
                }
            }
        }

        TetraEngine.Piece active = engine.getActive();
        if (active != null) {
            List<int[]> activeCells = engine.getActiveCells();
            for (int i = 0; i < activeCells.size(); i++) {
                int[] cellPos = activeCells.get(i);
                if (cellPos[1] >= 0) {
                    float jelly = 0.14f * (float) Math.sin(time * 7f + i * 1.1f + engine.getProgressToFall() * 5f);
                    float sway = 0.16f * (float) Math.cos(time * 4.3f + i * 0.8f);
                    drawJellyCell(canvas, cellPos[0], cellPos[1], active.type, cell, rowHeight, jelly, sway, true, false);
                }
            }
        }

        for (JellyParticle particle : particles) {
            float alpha = Math.max(0f, 1f - particle.age / particle.life);
            sparkPaint.setColor(withAlpha(PIECE_COLORS[particle.colorIndex], alpha * 0.95f));
            canvas.save();
            canvas.translate(particle.x, particle.y);
            canvas.rotate(particle.rotation);
            float stretch = Math.min(1.25f, Math.abs(particle.vy) / dp(420));
            float sx = particle.size * (1f + 0.34f * (float) Math.sin(particle.age * 9f + particle.phase) + stretch * 0.18f);
            float sy = particle.size * (1f - 0.16f * (float) Math.sin(particle.age * 8f + particle.phase) - stretch * 0.1f);
            canvas.scale(Math.max(0.72f, sx / particle.size), Math.max(0.72f, sy / particle.size));
            canvas.drawCircle(0, 0, particle.size, sparkPaint);
            sparkPaint.setColor(withAlpha(lighten(PIECE_COLORS[particle.colorIndex], 0.32f), alpha * 0.55f));
            canvas.drawCircle(-particle.size * 0.22f, -particle.size * 0.22f, particle.size * 0.42f, sparkPaint);
            canvas.restore();
        }
    }

    private void drawOverlay(Canvas canvas) {
        if (!useGestures) {
            buttonPaint.setTextSize(dp(26));
            drawButton(canvas, leftButton, "←", 0xCC143456, 0x6636D6FF);
            drawButton(canvas, rotateButton, "⟲", 0xCC2A2052, 0x66F47BF5);
            drawButton(canvas, rightButton, "→", 0xCC143456, 0x6636D6FF);
            drawButton(canvas, dropButton, "↓", 0xCC143456, 0x6636D6FF);

            helperPaint.setTextSize(dp(12));
            canvas.drawText("Move, rotate and drop. Every 6 cleared lines raises the speed.",
                    getWidth() * 0.5f, dropButton.bottom + dp(22), helperPaint);
        } else {
            helperPaint.setTextSize(dp(12));
            canvas.drawText("Swipe Left/Right to move, Swipe Up/Down to Drop, Tap to rotate.",
                    getWidth() * 0.5f, getHeight() - dp(32), helperPaint);
        }

        if (engine.isPaused()) {
            overlayPaint.setColor(0xBB000000);
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

            drawGlassCard(canvas, menuModalRect, 0xDD102136, 0x884ECFFF);
            
            overlayTextPaint.setTextSize(dp(28));
            canvas.drawText("MENU", menuModalRect.centerX(), menuModalRect.top + dp(45), overlayTextPaint);
            
            buttonPaint.setTextSize(dp(20));
            drawButton(canvas, continueBtnRect, "RESUME", 0xCC1D3952, 0x664ECFFF);
            drawButton(canvas, restartBtnRect, "RESTART", 0xCC4A2834, 0x66FF9A84);
            drawButton(canvas, homeBtnRect, "HOME", 0xCC2A2052, 0x66F47BF5);
        } else if (engine.isGameOver()) {
            overlayPaint.setColor(0xCC000000); // Full screen darkening
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

            drawGlassCard(canvas, menuModalRect, 0xDD250E19, 0x88FF9A84);
            overlayTextPaint.setTextSize(dp(30));
            canvas.drawText("BRICKS COLLAPSE", menuModalRect.centerX(), menuModalRect.top + dp(50), overlayTextPaint);
            
            // Score in Yellow
            hudLabelPaint.setColor(0xFFFFD35A);
            hudLabelPaint.setTextSize(dp(22));
            canvas.drawText("SCORE: " + engine.getScore(), menuModalRect.centerX(), menuModalRect.top + dp(90), hudLabelPaint);
            hudLabelPaint.setColor(Color.WHITE);
            
            buttonPaint.setTextSize(dp(20));
            // Reuse gameOver rects but centered in modal
            float mBtnW = menuModalRect.width() * 0.8f;
            float mBtnH = dp(54);
            float mBtnX = menuModalRect.left + (menuModalRect.width() - mBtnW) / 2f;
            gameOverRestartBtnRect.set(mBtnX, menuModalRect.top + dp(120), mBtnX + mBtnW, menuModalRect.top + dp(120) + mBtnH);
            gameOverMenuBtnRect.set(mBtnX, gameOverRestartBtnRect.bottom + dp(16), mBtnX + mBtnW, gameOverRestartBtnRect.bottom + dp(16) + mBtnH);

            drawButton(canvas, gameOverRestartBtnRect, "RESTART", 0xCC4A2834, 0x66FF9A84);
            drawButton(canvas, gameOverMenuBtnRect, "MAIN MENU", 0xCC2A2052, 0x66F47BF5);
        }
    }

    private void drawFloatingShapes(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();

        // Draw comets and particles with 70% opacity (178 out of 255)
        for (FloatingShape shape : floatingShapes) {
            float startX = shape.x * width;
            float startY = shape.y * height;
            int baseColor = COSMIC_COLORS[shape.colorIndex % COSMIC_COLORS.length];
            int headColor = withAlpha(baseColor, 0.70f);

            if (shape.type == 0) {
                // Comet trail
                float len = (float) Math.hypot(shape.vx * width, shape.vy * height);
                if (len > 0f) {
                    float dx = (shape.vx * width) / len;
                    float dy = (shape.vy * height) / len;
                    
                    float trailLength = dp(40f + 40f * shape.sizeScale);
                    float endX = startX - dx * trailLength;
                    float endY = startY - dy * trailLength;
                    
                    Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    trailPaint.setStyle(Paint.Style.STROKE);
                    trailPaint.setStrokeWidth(dp(2f + 2f * shape.sizeScale));
                    trailPaint.setStrokeCap(Paint.Cap.ROUND);
                    
                    int tailColor = 0x00000000;
                    Shader shader = new LinearGradient(
                            startX, startY, endX, endY,
                            headColor, tailColor,
                            Shader.TileMode.CLAMP
                    );
                    trailPaint.setShader(shader);
                    canvas.drawLine(startX, startY, endX, endY, trailPaint);
                }
                
                Paint headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                headPaint.setStyle(Paint.Style.FILL);
                headPaint.setColor(headColor);
                float radius = dp(3f + 3f * shape.sizeScale);
                canvas.drawCircle(startX, startY, radius, headPaint);
            } else {
                // Particle
                Paint partPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                partPaint.setStyle(Paint.Style.FILL);
                partPaint.setColor(headColor);
                float radius = dp(1.5f + 1.5f * shape.sizeScale);
                canvas.drawCircle(startX, startY, radius, partPaint);
            }
        }
    }

    private void drawMainMenu(Canvas canvas) {
        overlayPaint.setColor(0x44000000);
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

        // Premium Logo Rendering (PNG Logo) - 1.3x (reduced by 0.2), placed 30px lower with overlap prevention
        if (logoDrawable != null) {
            logoDrawable.setColorFilter(null);
            float aspect = (float)logoDrawable.getIntrinsicWidth() / logoDrawable.getIntrinsicHeight();
            float logoW = Math.min(dp(280) * 1.3f, getWidth() - dp(24));
            float logoH = logoW / aspect;
            
            // Safe top boundary (status bar height + padding)
            float logoTopBound = Math.max(insetTop + dp(35), getHeight() * 0.08f);
            float logoBottomBound = mmStartY - dp(15);
            float availableH = logoBottomBound - logoTopBound;
            
            // Scale down if it exceeds the available vertical space
            if (logoH > availableH) {
                logoH = availableH;
                logoW = logoH * aspect;
            }
            
            // Target center (shifted down from top)
            float logoCenterY = getHeight() * 0.22f + dp(30);
            
            // Clamp to ensure it doesn't overlap the top safe bound or the buttons
            float minY = logoTopBound + logoH / 2f;
            float maxY = logoBottomBound - logoH / 2f;
            if (minY > maxY) {
                logoCenterY = (logoTopBound + logoBottomBound) / 2f;
            } else {
                logoCenterY = Math.max(minY, Math.min(logoCenterY, maxY));
            }

            logoDrawable.setBounds((int)(getWidth() - logoW) / 2, (int)(logoCenterY - logoH/2), 
                                  (int)(getWidth() + logoW) / 2, (int)(logoCenterY + logoH/2));
            logoDrawable.draw(canvas);
        }
        
        // Footer text at the bottom safe zone
        if (!inOptions) {
            int originalColor = helperPaint.getColor();
            helperPaint.setColor(0xFFB293FE);
            helperPaint.setTextSize(dp(13));
            helperPaint.setAlpha(160);
            float footerY = getHeight() - insetBottom - dp(75);
            canvas.drawText("© 2026 Brick Space NEO", getWidth() / 2f, footerY, helperPaint);
            canvas.drawText("v1.0.3", getWidth() / 2f, footerY + dp(18), helperPaint);
            canvas.drawText("Build combos. Set records.", getWidth() / 2f, footerY + dp(36), helperPaint);
            helperPaint.setAlpha(255);
            helperPaint.setColor(originalColor);
        }
        
        buttonPaint.setTextSize(dp(22));
        if (inOptions) {
            float mmBtnWidth = startGameBtnRect.width();
            float mmBtnHeight = dp(52); 
            float mmBtnLeft = startGameBtnRect.left;
            float mmSpacing = dp(14); 
            float curY = mmStartY;
            
            RectF ctrlBtn = new RectF(mmBtnLeft, curY, mmBtnLeft + mmBtnWidth, curY + mmBtnHeight);
            curY += mmBtnHeight + mmSpacing;
            RectF musicBtn = new RectF(mmBtnLeft, curY, mmBtnLeft + mmBtnWidth, curY + mmBtnHeight);
            curY += mmBtnHeight + mmSpacing;
            RectF soundBtn = new RectF(mmBtnLeft, curY, mmBtnLeft + mmBtnWidth, curY + mmBtnHeight);
            curY += mmBtnHeight + mmSpacing;
            RectF backBtn = new RectF(mmBtnLeft, curY, mmBtnLeft + mmBtnWidth, curY + mmBtnHeight);
            
            drawButton(canvas, ctrlBtn, useGestures ? "Controls: Gestures" : "Controls: Buttons", 0xCC1D3952, 0x664ECFFF);
            drawButton(canvas, musicBtn, musicEnabled ? "Music: ON" : "Music: OFF", 0xCC2A2052, 0x66F47BF5);
            drawButton(canvas, soundBtn, soundsEnabled ? "Sounds: ON" : "Sounds: OFF", 0xCC2A2052, 0x66F47BF5);
            drawButton(canvas, backBtn, "Back", 0xCC4A2834, 0x66FF9A84);

            // Footer Reset Button
            buttonPaint.setTextSize(dp(18));
            drawButton(canvas, resetScoresBtnRect, "Reset Best Scores", 0xCC7A1020, 0x88FF3050);
            buttonPaint.setTextSize(dp(22));
            
            // Temporary rects for touch detection
            controlsToggleBtnRect.set(ctrlBtn);
            optionsMusicBtnRect.set(musicBtn);
            optionsSoundBtnRect.set(soundBtn);
            optionsGrayscaleBtnRect.setEmpty();
            optionsBackBtnActualRect.set(backBtn);
        } else {
            drawButton(canvas, startGameBtnRect, "Start Game", 0xCC1D3952, 0x664ECFFF);
            drawButton(canvas, optionsBtnRect, "Options", 0xCC2A2052, 0x66F47BF5);
            drawButton(canvas, exitBtnRect, "Exit", 0xCC4A2834, 0x66FF9A84);
        }
    }

    private void drawProgress(Canvas canvas) {
        float inset = dp(18);
        RectF bar = new RectF(progressRect.left + inset, progressRect.centerY() - dp(8), progressRect.right - inset, progressRect.centerY() + dp(8));
        // Higher contrast background
        glowPaint.setShader(null);
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setColor(0xAA000000); // Darker
        canvas.drawRoundRect(bar, dp(12), dp(12), glowPaint);

        float fill = engine.getCycleProgress();
        RectF fillRect = new RectF(bar.left, bar.top, bar.left + bar.width() * fill, bar.bottom);
        glowPaint.setShader(new LinearGradient(fillRect.left, fillRect.top, fillRect.right, fillRect.bottom,
                new int[]{0xFF009AFC, 0xFF1A33E0, 0xFF7912D3, 0xFFF21EE0}, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(fillRect, dp(12), dp(12), glowPaint);
        glowPaint.setShader(null);

        helperPaint.setTextSize(dp(11));
        String progressText = "LEVEL " + engine.getDisplayLevel() + " • " + engine.getLinesToNextLevel() + " LINES TO NEXT";
        canvas.drawText(progressText, progressRect.centerX(), progressRect.bottom - dp(16), helperPaint);
    }

    private void drawNextPiece(Canvas canvas) {
        TetraEngine.Piece piece = engine.getNext();
        if (piece == null) {
            return;
        }
        float previewCell = Math.min(nextRect.width(), nextRect.height()) / 5.6f;
        int[][] shape = TetraEngine.getShapeCells(piece.type, 0);

        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        for (int[] cell : shape) {
            if (cell[0] < minCol) minCol = cell[0];
            if (cell[0] > maxCol) maxCol = cell[0];
            if (cell[1] < minRow) minRow = cell[1];
            if (cell[1] > maxRow) maxRow = cell[1];
        }

        float shapeWidth = (maxCol - minCol + 1) * previewCell - dp(4);
        float shapeHeight = (maxRow - minRow + 1) * previewCell - dp(4);

        float availableTop = nextRect.top + dp(24);
        float availableHeight = nextRect.bottom - availableTop;

        float startX = nextRect.left + (nextRect.width() - shapeWidth) / 2f - minCol * previewCell;
        float startY = availableTop + (availableHeight - shapeHeight) / 2f - minRow * previewCell;

        for (int[] cell : shape) {
            float x = startX + cell[0] * previewCell;
            float y = startY + cell[1] * previewCell;
            RectF rect = new RectF(x, y, x + previewCell - dp(4), y + previewCell - dp(4));
            glowPaint.setColor(withAlpha(PIECE_COLORS[piece.type], 0.95f));
            canvas.drawRoundRect(rect, dp(10), dp(10), glowPaint);
        }
    }

    private void drawGlassCard(Canvas canvas, RectF rect, int topColor, int rimColor) {
        if (rect == mainScoreRect || rect == bestScoreRect || rect == nextRect || rect == progressRect || rect == snakeLevelBarRect || rect == snakeLivesPanelRect || rect == menuModalRect) {
            Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setStyle(Paint.Style.FILL);

            Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(dp(2f));

            if (rect == mainScoreRect || rect == bestScoreRect) {
                // SCORE and BEST specific style
                fillPaint.setColor(withAlpha(0xFF141335, 0.20f));
                canvas.drawRoundRect(rect, dp(22), dp(22), fillPaint);

                int[] strokeColors = new int[]{0xFF9432A9, 0xFF0C0C63, 0xFF6034E8};
                strokePaint.setShader(createAngledGradient(rect, strokeColors, null, -40f));
            } else if (rect == menuModalRect) {
                // Pause and Game Over modal backing: #141335, 85% opacity (slightly transparent), no outline
                fillPaint.setColor(withAlpha(0xFF141335, 0.85f));
                canvas.drawRoundRect(rect, dp(22), dp(22), fillPaint);
                return;
            } else {
                // Other HUD cards style
                int startColor = withAlpha(0xFF2A3780, 0.30f);
                int endColor = withAlpha(0xFF33439D, 0.30f);
                fillPaint.setShader(createAngledGradient(rect, new int[]{startColor, endColor}, null, -90f));
                canvas.drawRoundRect(rect, dp(22), dp(22), fillPaint);

                strokePaint.setColor(0xFF6188FF);
            }

            RectF strokeRect = new RectF(rect.left + dp(1), rect.top + dp(1), rect.right - dp(1), rect.bottom - dp(1));
            canvas.drawRoundRect(strokeRect, dp(21), dp(21), strokePaint);
            return;
        }
        
        int bottomColor = 0xD20C1527;
        boardPaint.setShader(new LinearGradient(rect.left, rect.top, rect.left, rect.bottom, topColor, bottomColor, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dp(22), dp(22), boardPaint);
        boardPaint.setShader(null);

        glowPaint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom, rimColor, 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(rect.left + dp(1), rect.top + dp(1), rect.right - dp(1), rect.bottom - dp(1)),
                dp(22), dp(22), glowPaint);
        glowPaint.setShader(null);
    }

    private void drawPlayfieldBackgroundAndStroke(Canvas canvas, RectF rect) {
        // Fill: #030b21 with 20% opacity
        boardPaint.setStyle(Paint.Style.FILL);
        boardPaint.setShader(null);
        boardPaint.setColor(withAlpha(0xFF030B21, 0.20f));
        canvas.drawRoundRect(rect, dp(26), dp(26), boardPaint);

        // Stroke: gradient #13bff7, #53ebfa, #0c0c63, #602fd6, #6034e8, angle 60 degrees
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(2.5f));
        int[] strokeColors = new int[]{0xFF13BFF7, 0xFF53EBFA, 0xFF0C0C63, 0xFF602FD6, 0xFF6034E8};
        strokePaint.setShader(createAngledGradient(rect, strokeColors, null, 60f));

        RectF strokeRect = new RectF(rect.left + dp(1), rect.top + dp(1), rect.right - dp(1), rect.bottom - dp(1));
        canvas.drawRoundRect(strokeRect, dp(25), dp(25), strokePaint);
    }

    private Shader createAngledGradient(RectF rect, int[] colors, float[] positions, float angleDegrees) {
        float cx = rect.centerX();
        float cy = rect.centerY();
        float r = (float) Math.hypot(rect.width(), rect.height()) / 2f;
        float angleRad = (float) Math.toRadians(angleDegrees);
        
        float dx = (float) Math.cos(angleRad);
        float dy = -(float) Math.sin(angleRad); // negative because Y is downwards in Android
        
        float x0 = cx - r * dx;
        float y0 = cy - r * dy;
        float x1 = cx + r * dx;
        float y1 = cy + r * dy;
        
        return new LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP);
    }

    private void drawButton(Canvas canvas, RectF rect, String label, int fillColor, int glowColor) {
        Paint btnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(2f));

        if (label.equalsIgnoreCase("Start Game") || rect == pauseButton || rect == rotateButton) {
            // Fill: gradient from #502a80 to #3d339d, 30% opacity, angle -30 degrees
            int startColor = withAlpha(0xFF502A80, 0.30f);
            int endColor = withAlpha(0xFF3D339D, 0.30f);
            btnPaint.setShader(createAngledGradient(rect, new int[]{startColor, endColor}, null, -30f));
            canvas.drawRoundRect(rect, dp(22), dp(22), btnPaint);

            // Stroke: gradient #009afc, #1a33e0, #7912d3, #f21ee0, angle 120 degrees, non-transparent
            int[] strokeColors = new int[]{0xFF009AFC, 0xFF1A33E0, 0xFF7912D3, 0xFFF21EE0};
            strokePaint.setShader(createAngledGradient(rect, strokeColors, null, 120f));
            
            RectF strokeRect = new RectF(rect.left + dp(1), rect.top + dp(1), rect.right - dp(1), rect.bottom - dp(1));
            canvas.drawRoundRect(strokeRect, dp(21), dp(21), strokePaint);
        } else {
            // All other UI buttons: gradient from #2a3780 and #33439d, 30% opacity, angle -90 degrees
            int startColor = withAlpha(0xFF2A3780, 0.30f);
            int endColor = withAlpha(0xFF33439D, 0.30f);
            btnPaint.setShader(createAngledGradient(rect, new int[]{startColor, endColor}, null, -90f));
            canvas.drawRoundRect(rect, dp(22), dp(22), btnPaint);

            // Border: solid #6188ff, non-transparent
            strokePaint.setColor(0xFF6188FF);
            RectF strokeRect = new RectF(rect.left + dp(1), rect.top + dp(1), rect.right - dp(1), rect.bottom - dp(1));
            canvas.drawRoundRect(strokeRect, dp(21), dp(21), strokePaint);
        }

        float baseline = rect.centerY() - (buttonPaint.descent() + buttonPaint.ascent()) * 0.5f;
        canvas.drawText(label, rect.centerX(), baseline, buttonPaint);
    }

    private void drawGhostCell(Canvas canvas, int col, int row, int colorIndex, float cell, float rowHeight) {
        RectF rect = cellRect(col, row, cell, rowHeight);
        int baseColor = PIECE_COLORS[colorIndex % PIECE_COLORS.length];
        glowPaint.setColor(withAlpha(baseColor, 0.16f));
        canvas.drawRoundRect(rect, dp(12), dp(12), glowPaint);
        blockStrokePaint.setColor(withAlpha(baseColor, 0.40f));
        canvas.drawRoundRect(rect, dp(12), dp(12), blockStrokePaint);
    }

    private void drawAutoScaledText(Canvas canvas, String text, float x, float y, float maxW, Paint paint, float baseSize) {
        paint.setTextSize(baseSize);
        float width = paint.measureText(text);
        if (width > maxW) {
            paint.setTextSize(baseSize * (maxW / width));
        }
        canvas.drawText(text, x, y, paint);
        paint.setTextSize(baseSize); // Restore
    }

    private int getCollapseColorIndex(int type) {
        switch (type) {
            case 0: return 3; // Green S-shape -> Lime/Green (index 3)
            case 1: return 5; // Purple T-shape -> Purple (index 5)
            case 2: return 4; // Magenta L-shape -> Magenta (index 4)
            case 3: return 0; // Blue Square -> Cyan/Blue (index 0)
            case 4: return 1; // Z-shape -> Orange (index 1)
            case 5: return 0; // Plus-cross -> Cyan/Blue (index 0)
            case 6: return 2; // I-bar -> Yellow (index 2)
            case 7: return 3; // U-shape -> Lime/Green (index 3)
            default: return type % PIECE_COLORS.length;
        }
    }

    private void drawJellyCellAt(Canvas canvas, RectF baseRect, int colorIndex, float squash, float sway, boolean active, boolean showPattern, boolean isGhost) {
        float inflateX = dp(2.2f) * squash;
        float inflateY = dp(3.1f) * sway;
        RectF rect = new RectF(baseRect.left - inflateX, baseRect.top + inflateY, baseRect.right + inflateX, baseRect.bottom - inflateY);
        float radius = dp(active ? 15 : 13);
        int baseColor = PIECE_COLORS[colorIndex % PIECE_COLORS.length];

        if (isGhost) {
            blockStrokePaint.setColor(withAlpha(baseColor, 0.45f));
            blockStrokePaint.setStyle(Paint.Style.STROKE);
            blockStrokePaint.setStrokeWidth(dp(2f));
            canvas.drawRoundRect(rect, radius, radius, blockStrokePaint);
            blockStrokePaint.setStrokeWidth(dp(1.3f));
            blockStrokePaint.setStyle(Paint.Style.FILL); // Restore
            return;
        }

        shadowPaint.setAlpha(active ? 120 : 70);
        canvas.drawRoundRect(new RectF(rect.left, rect.top + dp(4), rect.right, rect.bottom + dp(4)), radius, radius, shadowPaint);

        blockPaint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                lighten(baseColor, 0.22f), darken(baseColor, 0.18f), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, blockPaint);
        blockPaint.setShader(null);

        RectF sheenRect = new RectF(rect.left + dp(3), rect.top + dp(3), rect.right - dp(7), rect.top + rect.height() * 0.45f);
        blockShadePaint.setColor(withAlpha(lighten(baseColor, 0.34f), active ? 0.42f : 0.28f));
        canvas.drawRoundRect(sheenRect, radius, radius, blockShadePaint);

        blockStrokePaint.setColor(withAlpha(0xFFFFFFFF, active ? 0.42f : 0.24f));
        canvas.drawRoundRect(rect, radius, radius, blockStrokePaint);

        if (showPattern) {
            blockStrokePaint.setColor(0xFFFFFFFF);
            blockStrokePaint.setStrokeWidth(dp(2.2f));
            float cx = rect.centerX();
            float cy = rect.centerY();
            float sz = rect.width() * 0.22f;
            
            int pattern = colorIndex % 6;
            switch (pattern) {
                case 0: // Cross
                    canvas.drawLine(cx - sz, cy - sz, cx + sz, cy + sz, blockStrokePaint);
                    canvas.drawLine(cx + sz, cy - sz, cx - sz, cy + sz, blockStrokePaint);
                    break;
                case 1: // Vertical Bar
                    canvas.drawLine(cx, cy - sz * 1.3f, cx, cy + sz * 1.3f, blockStrokePaint);
                    break;
                case 2: // Horizontal Bar
                    canvas.drawLine(cx - sz * 1.3f, cy, cx + sz * 1.3f, cy, blockStrokePaint);
                    break;
                case 3: // Small Square
                    canvas.drawRect(cx - sz * 0.7f, cy - sz * 0.7f, cx + sz * 0.7f, cy + sz * 0.7f, blockStrokePaint);
                    break;
                case 4: // Dot
                    canvas.drawCircle(cx, cy, sz * 0.6f, blockStrokePaint);
                    break;
                case 5: // Plus
                    canvas.drawLine(cx - sz, cy, cx + sz, cy, blockStrokePaint);
                    canvas.drawLine(cx, cy - sz, cx, cy + sz, blockStrokePaint);
                    break;
            }
        }
    }

    private void drawJellyCell(Canvas canvas, int col, int row, int colorIndex, float cell, float rowHeight,
                               float squash, float sway, boolean active, boolean showPattern) {
        RectF baseRect = cellRect(col, row, cell, rowHeight);
        drawJellyCellAt(canvas, baseRect, colorIndex, squash, sway, active, showPattern, false);
    }

    private RectF cellRect(int col, int row, float cell, float rowHeight) {
        float padX = dp(2.5f);
        float padY = dp(2.5f);
        float left = boardRect.left + col * cell + padX;
        float top = boardRect.top + row * rowHeight + padY;
        return new RectF(left, top, left + cell - padX * 2f, top + rowHeight - padY * 2f);
    }

    private void emitLineBurst(int row, int[] colors, float cellSize) {
        for (int col = 0; col < colors.length; col++) {
            if (colors[col] == 0) {
                continue;
            }
            float centerX = boardRect.left + cellSize * (col + 0.5f);
            float centerY = boardRect.top + boardRect.height() / TetraEngine.ROWS * (row + 0.5f);
            for (int i = 0; i < 7; i++) {
                JellyParticle particle = new JellyParticle();
                float angle = (float) (-2.6f + random.nextFloat() * 5.2f);
                float speed = dp(130 + random.nextFloat() * 250f);
                particle.x = centerX;
                particle.y = centerY;
                particle.vx = (float) Math.cos(angle) * speed;
                particle.vy = (float) Math.sin(angle) * speed - dp(120 + random.nextFloat() * 140f);
                particle.size = dp(4.5f + random.nextFloat() * 4.5f);
                particle.life = 1.4f + random.nextFloat() * 0.9f;
                int rawColorIdx = Math.max(0, colors[col] - 1);
                particle.colorIndex = (currentGameMode == GameMode.COLLAPSE) ? getCollapseColorIndex(rawColorIdx) : rawColorIdx;
                particle.phase = random.nextFloat() * 6.28f;
                particle.spin = -180f + random.nextFloat() * 360f;
                particle.gravity = dp(640 + random.nextFloat() * 180f);
                particle.drag = 0.9f + random.nextFloat() * 1.1f;
                particle.bounce = 0.42f + random.nextFloat() * 0.22f;
                particle.floorFriction = 0.84f + random.nextFloat() * 0.1f;
                particles.add(particle);
            }
        }
    }

    private void emitBurst(float x, float y, int count, float intensity) {
        emitBurst(x, y, count, intensity, -1);
    }

    private void emitBurst(float x, float y, int count, float intensity, int colorIndex) {
        for (int i = 0; i < count; i++) {
            JellyParticle particle = new JellyParticle();
            float angle = random.nextFloat() * 6.28318f;
            float speed = dp(70 + random.nextFloat() * 180f) * intensity;
            particle.x = x;
            particle.y = y;
            particle.vx = (float) Math.cos(angle) * speed;
            particle.vy = (float) Math.sin(angle) * speed - dp(60);
            particle.size = dp(4 + random.nextFloat() * 7);
            particle.life = 0.7f + random.nextFloat() * 0.5f;
            particle.colorIndex = colorIndex >= 0 ? colorIndex : random.nextInt(PIECE_COLORS.length);
            particle.phase = random.nextFloat() * 6.28f;
            particle.spin = -140f + random.nextFloat() * 280f;
            particle.gravity = dp(520 + random.nextFloat() * 160f);
            particle.drag = 0.8f + random.nextFloat() * 0.9f;
            particle.bounce = 0.36f + random.nextFloat() * 0.2f;
            particle.floorFriction = 0.82f + random.nextFloat() * 0.12f;
            particles.add(particle);
        }
    }

    private void triggerLinePull(int clearedLines) {
        float intensity = Math.min(1.5f, 0.55f + clearedLines * 0.22f);
        orbLinePull = Math.max(orbLinePull, intensity);
        for (int i = 0; i < orbs.size(); i++) {
            BackgroundOrb orb = orbs.get(i);
            orb.pullVelocity += dp(80 + random.nextFloat() * 110f) * intensity;
            orb.pullOffset += dp(6 + random.nextFloat() * 12f) * intensity;
        }
    }

    private void drawBlob(Canvas canvas, float centerX, float centerY, float size, int color, float time) {
        blobPath.reset();
        int points = 10;
        for (int i = 0; i < points; i++) {
            float angle = (float) (i * Math.PI * 2f / points);
            float radius = size * (0.8f + 0.12f * (float) Math.sin(time + angle * 2.3f));
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius * 0.76f;
            if (i == 0) {
                blobPath.moveTo(x, y);
            } else {
                blobPath.lineTo(x, y);
            }
        }
        blobPath.close();
        glowPaint.setColor(color);
        canvas.drawPath(blobPath, glowPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchDownX = x;
            touchDownY = y;
            touchAnchorX = x;
            isSwipingHorizontally = false;

            if (inMainMenu) {
                handleMainMenuTouch(x, y);
                return true;
            }

            if (inGameSelection) {
                handleGameSelectionTouch(x, y);
                return true;
            }

            if (currentGameMode == GameMode.BRICKS) {
                handleBricksTouch(x, y);
            } else if (currentGameMode == GameMode.SNAKE) {
                handleSnakeTouch(x, y);
            } else if (currentGameMode == GameMode.COLUMNS) {
                handleColumnsTouch(x, y);
            } else if (currentGameMode == GameMode.COLLAPSE) {
                handleCollapseTouch(x, y);
            } else if (currentGameMode == GameMode.BREAKER) {
                handleBreakerTouch(x, y, MotionEvent.ACTION_DOWN);
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (currentGameMode == GameMode.BREAKER && !inMainMenu && !inGameSelection) {
                handleBreakerTouch(x, y, MotionEvent.ACTION_MOVE);
            } else if (useGestures && !inMainMenu && !inGameSelection) {
                if (currentGameMode == GameMode.BRICKS) handleBricksGestureMove(x, y);
                else if (currentGameMode == GameMode.COLUMNS) handleColumnsGestureMove(x, y);
                else if (currentGameMode == GameMode.COLLAPSE) handleCollapseGestureMove(x, y);
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (currentGameMode == GameMode.BREAKER && !inMainMenu && !inGameSelection) {
                handleBreakerTouch(x, y, event.getAction());
            } else if (useGestures && !inMainMenu && !inGameSelection) {
                if (currentGameMode == GameMode.BRICKS) handleBricksGestureUp(x, y);
                else if (currentGameMode == GameMode.SNAKE) handleSnakeGestureUp(x, y);
                else if (currentGameMode == GameMode.COLUMNS) handleColumnsGestureUp(x, y);
                else if (currentGameMode == GameMode.COLLAPSE) handleCollapseGestureUp(x, y);
            }
        }
        return true;
    }

    private void handleMainMenuTouch(float x, float y) {
        if (inOptions) {
            if (contains(controlsToggleBtnRect, x, y)) {
                useGestures = !useGestures;
                playSound(soundRotate);
            } else if (contains(optionsMusicBtnRect, x, y)) {
                musicEnabled = !musicEnabled;
                if (musicEnabled) {
                    if (bgMusic != null) bgMusic.start();
                } else {
                    if (bgMusic != null && bgMusic.isPlaying()) bgMusic.pause();
                }
                getContext().getSharedPreferences("BrickSpaceNeoPrefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("MUSIC_ENABLED", musicEnabled).apply();
                playSound(soundRotate);
            } else if (contains(optionsSoundBtnRect, x, y)) {
                soundsEnabled = !soundsEnabled;
                getContext().getSharedPreferences("BrickSpaceNeoPrefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("SOUNDS_ENABLED", soundsEnabled).apply();
                playSound(soundRotate);
            } else if (contains(resetScoresBtnRect, x, y)) {
                bestScore = 0;
                bestScoreSnake = 0;
                bestScoreColumns = 0;
                bestScoreCollapse = 0;
                bestScoreBreaker = 0;
                getContext().getSharedPreferences("BrickSpaceNeoPrefs", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("BEST_SCORE", 0)
                        .putInt("BEST_SCORE_SNAKE", 0)
                        .putInt("BEST_SCORE_COLUMNS", 0)
                        .putInt("BEST_SCORE_COLLAPSE", 0)
                        .putInt("BEST_SCORE_BREAKER", 0)
                        .apply();
                playSound(soundGameOver);
            } else if (contains(optionsBackBtnActualRect, x, y)) {
                inOptions = false;
                playSound(soundRotate);
            }
        } else {
            if (contains(startGameBtnRect, x, y)) {
                inMainMenu = false;
                inGameSelection = true;
                playSound(soundRotate);
            } else if (contains(optionsBtnRect, x, y)) {
                inOptions = true;
                playSound(soundRotate);
            } else if (contains(exitBtnRect, x, y)) {
                if (getContext() instanceof android.app.Activity) {
                    ((android.app.Activity) getContext()).finish();
                }
            }
        }
    }

    private void handleGameSelectionTouch(float x, float y) {
        if (contains(bricksTileRect, x, y)) {
            currentGameMode = GameMode.BRICKS;
            startGame();
            playSound(soundRotate);
        } else if (contains(snakeTileRect, x, y)) {
            currentGameMode = GameMode.SNAKE;
            startGame();
            playSound(soundRotate);
        } else if (contains(columnsTileRect, x, y)) {
            currentGameMode = GameMode.COLUMNS;
            startGame();
            playSound(soundRotate);
        } else if (contains(collapseTileRect, x, y)) {
            currentGameMode = GameMode.COLLAPSE;
            startGame();
            playSound(soundRotate);
        } else if (contains(breakerTileRect, x, y)) {
            currentGameMode = GameMode.BREAKER;
            startGame();
            playSound(soundRotate);
        } else if (contains(gameSelectionBackBtnRect, x, y)) {
            inGameSelection = false;
            inMainMenu = true;
            playSound(soundRotate);
        }
    }

    private void startGame() {
        inGameSelection = false;
        particles.clear();
        boardShake = 0f;
        prevScore = 0;
        prevLines = 0;
        prevGameOver = false;
        if (currentGameMode == GameMode.BRICKS) {
            engine.restart();
        } else if (currentGameMode == GameMode.SNAKE) {
            snakeEngine.resetGame();
        } else if (currentGameMode == GameMode.COLUMNS) {
            columnsEngine.resetGame();
        } else if (currentGameMode == GameMode.COLLAPSE) {
            collapseEngine.restart();
        } else {
            breakerEngine.restart();
        }
        restartMusic();
    }

    private void handleBricksTouch(float x, float y) {
        if (engine.isPaused()) {
            if (contains(continueBtnRect, x, y)) {
                engine.togglePause();
                playSound(soundRotate);
            } else if (contains(restartBtnRect, x, y)) {
                startGame();
                playSound(soundRotate);
            } else if (contains(homeBtnRect, x, y)) {
                inMainMenu = true;
                engine.togglePause();
                updateMusic();
                playSound(soundRotate);
            }
            return;
        }

        if (engine.isGameOver()) {
            if (contains(gameOverRestartBtnRect, x, y)) {
                startGame();
                playSound(soundRotate);
            } else if (contains(gameOverMenuBtnRect, x, y)) {
                inMainMenu = true;
                updateMusic();
                playSound(soundRotate);
            }
            return;
        }

        if (contains(pauseButton, x, y)) {
            engine.togglePause();
            playSound(soundRotate);
            return;
        }

        if (!useGestures) {
            if (contains(leftButton, x, y)) {
                if (engine.move(-1)) boardShake = 0.2f;
            } else if (contains(rightButton, x, y)) {
                if (engine.move(1)) boardShake = 0.2f;
            } else if (contains(rotateButton, x, y)) {
                if (engine.rotate()) {
                    emitBurst(boardRect.centerX(), boardRect.top + dp(70), 4, 0.5f);
                    playSound(soundRotate);
                }
            } else if (contains(dropButton, x, y)) {
                engine.hardDrop();
                boardShake = 0.55f;
                playSound(soundDrop);
            } else if (boardRect.contains(x, y)) {
                float zone = boardRect.width() / 3f;
                if (y > boardRect.centerY() + dp(40)) {
                    engine.softDrop();
                    playSound(soundRotate);
                } else if (x < boardRect.left + zone) {
                    engine.move(-1);
                } else if (x > boardRect.right - zone) {
                    engine.move(1);
                } else {
                    if (engine.rotate()) playSound(soundRotate);
                }
            }
        }
    }

    private void handleSnakeTouch(float x, float y) {
        if (snakeEngine.isPaused()) {
            if (contains(continueBtnRect, x, y)) {
                snakeEngine.togglePause();
                playSound(soundRotate);
            } else if (contains(restartBtnRect, x, y)) {
                startGame();
                playSound(soundRotate);
            } else if (contains(homeBtnRect, x, y)) {
                inMainMenu = true;
                snakeEngine.togglePause();
                updateMusic();
                playSound(soundRotate);
            }
            return;
        }

        if (snakeEngine.isGameOver()) {
            if (contains(gameOverRestartBtnRect, x, y)) {
                startGame();
                playSound(soundRotate);
            } else if (contains(gameOverMenuBtnRect, x, y)) {
                inMainMenu = true;
                updateMusic();
                playSound(soundRotate);
            }
            return;
        }

        if (contains(pauseButton, x, y)) {
            snakeEngine.togglePause();
            playSound(soundRotate);
            return;
        }

        if (!useGestures) {
            if (contains(leftButton, x, y)) {
                snakeEngine.setDirection(-1, 0);
            } else if (contains(rightButton, x, y)) {
                snakeEngine.setDirection(1, 0);
            } else if (contains(dropButton, x, y)) {
                snakeEngine.setDirection(0, 1);
            } else if (contains(rotateButton, x, y)) {
                snakeEngine.setDirection(0, -1);
            }
        }
    }

    private void handleSnakeGestureUp(float x, float y) {
        if (contains(pauseButton, touchDownX, touchDownY)) return;
        float dx = x - touchDownX;
        float dy = y - touchDownY;
        float threshold = dp(20);
        
        if (Math.abs(dx) > Math.abs(dy)) {
            if (Math.abs(dx) > threshold) {
                if (dx > 0) snakeEngine.setDirection(1, 0);
                else snakeEngine.setDirection(-1, 0);
            }
        } else {
            if (Math.abs(dy) > threshold) {
                if (dy > 0) snakeEngine.setDirection(0, 1);
                else snakeEngine.setDirection(0, -1);
            }
        }
    }

    private void handleBricksGestureMove(float x, float y) {
        if (contains(pauseButton, touchDownX, touchDownY)) return;
        float totalDx = x - touchDownX;
        float totalDy = y - touchDownY;
        float dx = x - touchAnchorX;
        float step = dp(35);
        if (!isSwipingHorizontally && Math.abs(totalDx) > dp(15) && Math.abs(totalDx) > Math.abs(totalDy)) {
            isSwipingHorizontally = true;
        }
        if (isSwipingHorizontally) {
            if (dx > step) {
                if (engine.move(1)) boardShake = 0.2f;
                touchAnchorX += step;
            } else if (dx < -step) {
                if (engine.move(-1)) boardShake = 0.2f;
                touchAnchorX -= step;
            }
        }
    }

    private void handleBricksGestureUp(float x, float y) {
        if (contains(pauseButton, touchDownX, touchDownY)) return;
        if (!isSwipingHorizontally) {
            float dx = x - touchDownX;
            float dy = y - touchDownY;
            float threshold = dp(20);
            if (Math.abs(dx) < threshold && Math.abs(dy) < threshold) {
                if (engine.rotate()) {
                    emitBurst(boardRect.centerX(), boardRect.top + dp(70), 4, 0.5f);
                    playSound(soundRotate);
                }
            } else if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > threshold) {
                engine.hardDrop();
                boardShake = 0.55f;
                playSound(soundDrop);
            }
        }
    }

    private void handleColumnsGestureMove(float x, float y) {
        if (contains(pauseButton, touchDownX, touchDownY)) return;
        float totalDx = x - touchDownX;
        float totalDy = y - touchDownY;
        float dx = x - touchAnchorX;
        float step = dp(35);
        if (!isSwipingHorizontally && Math.abs(totalDx) > dp(15) && Math.abs(totalDx) > Math.abs(totalDy)) {
            isSwipingHorizontally = true;
        }
        if (isSwipingHorizontally) {
            if (dx > step) {
                if (columnsEngine.move(1)) boardShake = 0.15f;
                touchAnchorX += step;
            } else if (dx < -step) {
                if (columnsEngine.move(-1)) boardShake = 0.15f;
                touchAnchorX -= step;
            }
        }
    }

    private void handleColumnsGestureUp(float x, float y) {
        if (contains(pauseButton, touchDownX, touchDownY)) return;
        if (!isSwipingHorizontally) {
            float dx = x - touchDownX;
            float dy = y - touchDownY;
            float threshold = dp(20);
            if (Math.abs(dx) < threshold && Math.abs(dy) < threshold) {
                // TAP -> Change sequence (rotate)
                if (!columnsEngine.isResolving() && !columnsEngine.isGameOver()) {
                    columnsEngine.rotate();
                    emitBurst(boardRect.centerX(), boardRect.top + dp(70), 4, 0.5f);
                    playSound(soundRotate);
                }
            } else if (dy < -threshold * 1.5f && Math.abs(dy) > Math.abs(dx)) {
                // SWIPE UP -> Hard Drop
                columnsEngine.fastDrop();
                boardShake = 0.55f;
                playSound(soundDrop);
            }
        }
    }

    private void handleCollapseTouch(float x, float y) {
        if (collapseEngine.isPaused()) {
            if (contains(continueBtnRect, x, y)) {
                collapseEngine.togglePause();
                playSound(soundRotate);
            } else if (contains(restartBtnRect, x, y)) {
                startGame();
                playSound(soundRotate);
            } else if (contains(homeBtnRect, x, y)) {
                inMainMenu = true;
                collapseEngine.togglePause();
                updateMusic();
                playSound(soundRotate);
            }
            return;
        }

        if (collapseEngine.isGameOver()) {
            if (contains(gameOverRestartBtnRect, x, y)) {
                startGame();
                playSound(soundRotate);
            } else if (contains(gameOverMenuBtnRect, x, y)) {
                inMainMenu = true;
                updateMusic();
                playSound(soundRotate);
            }
            return;
        }

        if (contains(pauseButton, x, y)) {
            collapseEngine.togglePause();
            playSound(soundRotate);
            return;
        }

        if (!useGestures) {
            if (contains(leftButton, x, y)) {
                if (collapseEngine.move(-1)) boardShake = 0.2f;
            } else if (contains(rightButton, x, y)) {
                if (collapseEngine.move(1)) boardShake = 0.2f;
            } else if (contains(rotateButton, x, y)) {
                if (collapseEngine.rotate()) {
                    emitBurst(boardRect.centerX(), boardRect.top + dp(70), 4, 0.5f);
                    playSound(soundRotate);
                }
            } else if (contains(dropButton, x, y)) {
                collapseEngine.hardDrop();
                boardShake = 0.55f;
                playSound(soundDrop);
            } else if (boardRect.contains(x, y)) {
                float zone = boardRect.width() / 3f;
                if (y > boardRect.centerY() + dp(40)) {
                    collapseEngine.softDrop();
                    playSound(soundRotate);
                } else if (x < boardRect.left + zone) {
                    collapseEngine.move(-1);
                } else if (x > boardRect.right - zone) {
                    collapseEngine.move(1);
                } else {
                    if (collapseEngine.rotate()) playSound(soundRotate);
                }
            }
        }
    }

    private void handleCollapseGestureMove(float x, float y) {
        if (contains(pauseButton, touchDownX, touchDownY)) return;
        float totalDx = x - touchDownX;
        float totalDy = y - touchDownY;
        float dx = x - touchAnchorX;
        float step = dp(35);
        if (!isSwipingHorizontally && Math.abs(totalDx) > dp(15) && Math.abs(totalDx) > Math.abs(totalDy)) {
            isSwipingHorizontally = true;
        }
        if (isSwipingHorizontally) {
            if (dx > step) {
                if (collapseEngine.move(1)) boardShake = 0.2f;
                touchAnchorX += step;
            } else if (dx < -step) {
                if (collapseEngine.move(-1)) boardShake = 0.2f;
                touchAnchorX -= step;
            }
        }
    }

    private void handleCollapseGestureUp(float x, float y) {
        if (contains(pauseButton, touchDownX, touchDownY)) return;
        if (!isSwipingHorizontally) {
            float dx = x - touchDownX;
            float dy = y - touchDownY;
            float threshold = dp(20);
            if (Math.abs(dx) < threshold && Math.abs(dy) < threshold) {
                if (collapseEngine.rotate()) {
                    emitBurst(boardRect.centerX(), boardRect.top + dp(70), 4, 0.5f);
                    playSound(soundRotate);
                }
            } else if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > threshold) {
                collapseEngine.hardDrop();
                boardShake = 0.55f;
                playSound(soundDrop);
            }
        }
    }

    private void drawGameSelection(Canvas canvas) {
        overlayPaint.setColor(0x44000000);
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

        titlePaint.setTextSize(dp(28));
        canvas.drawText("SELECT GAME", getWidth() / 2f, bricksTileRect.top - dp(40), titlePaint);

        // Bricks Tile
        drawGameTile(canvas, bricksTileRect, "BRICKS", 0, currentGameMode == GameMode.BRICKS);
        // Snake Tile
        drawGameTile(canvas, snakeTileRect, "SNAKE", 1, currentGameMode == GameMode.SNAKE);
        // Columns Tile
        drawGameTile(canvas, columnsTileRect, "COLUMNS", 2, currentGameMode == GameMode.COLUMNS);
        // Collapse Tile
        drawGameTile(canvas, collapseTileRect, "COLLAPSE", 3, currentGameMode == GameMode.COLLAPSE);
        // Breaker Tile
        drawGameTile(canvas, breakerTileRect, "BREAKER", 4, currentGameMode == GameMode.BREAKER);

        drawButton(canvas, gameSelectionBackBtnRect, "BACK", 0xCC4A2834, 0x66FF9A84);
    }

    private void drawGameTile(Canvas canvas, RectF rect, String title, int type, boolean selected) {
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        int startColor = withAlpha(0xFF2A3780, 0.30f);
        int endColor = withAlpha(0xFF33439D, 0.30f);
        fillPaint.setShader(createAngledGradient(rect, new int[]{startColor, endColor}, null, -90f));
        canvas.drawRoundRect(rect, dp(22), dp(22), fillPaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(2f));
        strokePaint.setColor(selected ? 0xFF4ECFFF : 0xFF6188FF);
        RectF strokeRect = new RectF(rect.left + dp(1), rect.top + dp(1), rect.right - dp(1), rect.bottom - dp(1));
        canvas.drawRoundRect(strokeRect, dp(21), dp(21), strokePaint);

        float iconSize = dp(40);
        float iconX = rect.centerX();
        float iconY = rect.top + dp(60);
        
        if (type == 0) { // Bricks Icon
            drawJellyCellAt(canvas, new RectF(iconX - iconSize/2, iconY - iconSize/2, iconX + iconSize/2, iconY + iconSize/2), 0, 0, 0, true, false, false);
        } else if (type == 1) { // Snake Icon
            glowPaint.setColor(PIECE_COLORS[3]); // Green
            canvas.drawCircle(iconX, iconY, iconSize/2, glowPaint);
            glowPaint.setColor(Color.WHITE);
            canvas.drawCircle(iconX - dp(8), iconY - dp(5), dp(4), glowPaint);
            canvas.drawCircle(iconX + dp(8), iconY - dp(5), dp(4), glowPaint);
        } else if (type == 2) { // Columns Icon
            float bw = iconSize * 1.5f;
            float bh = iconSize * 0.45f;
            RectF cr = new RectF(iconX - bw/2, iconY - bh/2, iconX + bw/2, iconY + bh/2);
            drawGlassCard(canvas, cr, 0xCC143456, 0x6636D6FF);
            float cs = bw / 4.5f;
            for (int i = 0; i < 4; i++) {
                float cx = cr.left + dp(6) + i * (cs + dp(4));
                glowPaint.setColor(PIECE_COLORS[i % PIECE_COLORS.length]);
                canvas.drawCircle(cx + cs/2, iconY, cs/2.2f, glowPaint);
            }
        } else if (type == 3) { // Collapse Icon
            float bs = iconSize * 0.35f;
            drawJellyCellAt(canvas, new RectF(iconX - bs * 1.2f, iconY + bs * 0.4f, iconX - bs * 0.2f, iconY + bs * 1.4f), 1, 0, 0, true, false, false);
            drawJellyCellAt(canvas, new RectF(iconX - bs * 0.5f, iconY - bs * 0.5f, iconX + bs * 0.5f, iconY + bs * 0.5f), 2, 0, 0, true, false, false);
            drawJellyCellAt(canvas, new RectF(iconX + bs * 0.2f, iconY - bs * 1.4f, iconX + bs * 1.2f, iconY - bs * 0.4f), 3, 0, 0, true, false, false);
        } else { // Breaker Icon
            // Draw a tiny paddle at the bottom
            float pw = iconSize * 1.1f;
            float ph = iconSize * 0.22f;
            RectF padR = new RectF(iconX - pw/2, iconY + iconSize/4 - ph/2, iconX + pw/2, iconY + iconSize/4 + ph/2);
            blockPaint.setColor(0xFF4ECFFF);
            canvas.drawRoundRect(padR, dp(3), dp(3), blockPaint);
            
            // Draw a tiny ball bouncing upwards
            float bx = iconX;
            float by = iconY - iconSize/4;
            glowPaint.setColor(0xFFFFD35A);
            canvas.drawCircle(bx, by, dp(5), glowPaint);
        }

        buttonPaint.setTextSize(dp(22));
        canvas.drawText(title, rect.centerX(), rect.bottom - dp(30), buttonPaint);
    }

    private void drawSnakeHud(Canvas canvas) {
        drawButton(canvas, pauseButton, "", 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, mainScoreRect, 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, bestScoreRect, 0xCC143456, 0x6636D6FF);

        // Pause Button
        float cx = pauseButton.centerX();
        float cy = pauseButton.centerY();
        float pw = dp(4);
        float ph = dp(16);
        float pgap = dp(3);
        buttonPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(cx - pgap - pw, cy - ph/2f, cx - pgap, cy + ph/2f, dp(1), dp(1), buttonPaint);
        canvas.drawRoundRect(cx + pgap, cy - ph/2f, cx + pgap + pw, cy + ph/2f, dp(1), dp(1), buttonPaint);

        // Score & Combo (Dynamic)
        hudLabelPaint.setTextAlign(Paint.Align.CENTER);
        hudValuePaint.setTextAlign(Paint.Align.CENTER);
        hudLabelPaint.setTextSize(dp(11)); // Match SPEED FLOW
        
        float scoreY = mainScoreRect.top + dp(65);
        if (snakeEngine.getCombo() > 1) {
            scoreY = mainScoreRect.top + dp(58);
            hudLabelPaint.setColor(0xFFFFD35A); // Gold
            canvas.drawText("x" + snakeEngine.getCombo() + " COMBO", mainScoreRect.centerX(), mainScoreRect.bottom - dp(12), hudLabelPaint);
            hudLabelPaint.setColor(Color.WHITE);
        }
        
        hudValuePaint.setTextSize(dp(32));
        hudLabelPaint.setTextSize(dp(11));
        canvas.drawText("SCORE", mainScoreRect.centerX(), mainScoreRect.top + dp(22), hudLabelPaint);
        drawAutoScaledText(canvas, String.valueOf(snakeEngine.getScore()), mainScoreRect.centerX(), scoreY, mainScoreRect.width() - dp(20), hudValuePaint, dp(32));

        // Best
        hudLabelPaint.setTextAlign(Paint.Align.LEFT);
        hudValuePaint.setTextAlign(Paint.Align.LEFT);
        hudLabelPaint.setTextSize(dp(11));
        canvas.drawText("BEST", bestScoreRect.left + dp(44), bestScoreRect.top + dp(20), hudLabelPaint);
        int bScore = (currentGameMode == GameMode.BRICKS) ? bestScore : bestScoreSnake;
        drawAutoScaledText(canvas, String.valueOf(bScore), bestScoreRect.left + dp(44), bestScoreRect.centerY() + dp(12), bestScoreRect.width() - dp(52), hudValuePaint, dp(18));
        canvas.drawText("🏆", bestScoreRect.left + dp(22), bestScoreRect.centerY() - (buttonPaint.descent() + buttonPaint.ascent()) / 2, buttonPaint);
    }

    private void drawSnakeBoard(Canvas canvas, float time) {
        // Board Background
        drawPlayfieldBackgroundAndStroke(canvas, boardRect);

        float cellW = boardRect.width() / SnakeEngine.COLS;
        float cellH = boardRect.height() / SnakeEngine.ROWS;

        // Grid (Dots/Circles as in screenshot)
        gridPaint.setColor(0x18FFFFFF);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1));
        float dotRadius = Math.min(cellW, cellH) * 0.35f;
        for (int r = 0; r < SnakeEngine.ROWS; r++) {
            for (int c = 0; c < SnakeEngine.COLS; c++) {
                float cx = boardRect.left + c * cellW + cellW / 2;
                float cy = boardRect.top + r * cellH + cellH / 2;
                canvas.drawCircle(cx, cy, dotRadius, gridPaint);
            }
        }

        // Food (Pink circle)
        SnakeEngine.Point food = snakeEngine.getFood();
        if (food != null) {
            float fx = boardRect.left + food.x * cellW + cellW / 2;
            float fy = boardRect.top + food.y * cellH + cellH / 2;
            float pulse = 1f + 0.15f * (float)Math.sin(time * 6f);
            int foodColor = 0xFFFF2D55;
            glowPaint.setColor(foodColor); // Pink/Red or White for DMG
            canvas.drawCircle(fx, fy, (cellW / 2 - dp(2)) * pulse, glowPaint);
            
            // Subtle glow
            glowPaint.setAlpha(120);
            canvas.drawCircle(fx, fy, (cellW / 2 + dp(4)) * pulse, glowPaint);
            glowPaint.setAlpha(255);
        }

        // Snake (Green circles)
        List<SnakeEngine.Point> body = snakeEngine.getSnake();
        for (int i = 0; i < body.size(); i++) {
            SnakeEngine.Point p = body.get(i);
            float cx = boardRect.left + p.x * cellW + cellW / 2;
            float cy = boardRect.top + p.y * cellH + cellH / 2;
            float radius = cellW / 2 - dp(1);
            
            glowPaint.setColor(0xFF4CD964); // Bright Green from screenshot
            canvas.drawCircle(cx, cy, radius, glowPaint);
            
            if (i == 0) { // Head detail
                glowPaint.setColor(0xFF248A3D);
                canvas.drawCircle(cx, cy, radius * 0.4f, glowPaint);
            }
        }

        // Level Bar (Right side)
        drawSnakeLevelBar(canvas);
        
        // Particles
        for (JellyParticle particle : particles) {
            float alpha = Math.max(0f, 1f - particle.age / particle.life);
            sparkPaint.setColor(withAlpha(PIECE_COLORS[particle.colorIndex], alpha * 0.95f));
            canvas.drawCircle(particle.x, particle.y, particle.size, sparkPaint);
        }
    }

    private void drawSnakeLevelBar(Canvas canvas) {
        drawGlassCard(canvas, snakeLevelBarRect, 0xCC143456, 0x6636D6FF);
        
        hudLabelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("SPEED FLOW", snakeLevelBarRect.centerX(), snakeLevelBarRect.top + dp(18), hudLabelPaint);

        float barInset = dp(20);
        float barHeight = dp(14);
        RectF bar = new RectF(snakeLevelBarRect.left + barInset, snakeLevelBarRect.centerY() - barHeight/2, 
                             snakeLevelBarRect.right - barInset, snakeLevelBarRect.centerY() + barHeight/2);
        
        // Bar background
        overlayPaint.setColor(0x44000000);
        canvas.drawRoundRect(bar, barHeight/2, barHeight/2, overlayPaint);
        
        float progress = (snakeEngine.getFoodEatenCount() % 3) / 3f;
        RectF fill = new RectF(bar.left, bar.top, bar.left + bar.width() * progress, bar.bottom);
        glowPaint.setShader(new LinearGradient(fill.left, fill.top, fill.right, fill.bottom,
                new int[]{0xFF009AFC, 0xFF1A33E0, 0xFF7912D3, 0xFFF21EE0}, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(fill, barHeight/2, barHeight/2, glowPaint);
        glowPaint.setShader(null);
        
        helperPaint.setTextSize(dp(11));
        String text = "LEVEL " + snakeEngine.getLevel() + " • " + (3 - snakeEngine.getFoodEatenCount() % 3) + " FOOD TO NEXT";
        canvas.drawText(text, snakeLevelBarRect.centerX(), snakeLevelBarRect.bottom - dp(16), helperPaint);
    }

    private void drawSnakeOverlay(Canvas canvas) {
        // Lives Panel
        drawGlassCard(canvas, snakeLivesPanelRect, 0xCC143456, 0x6636D6FF);
        hudLabelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("LIVES", snakeLivesPanelRect.centerX(), snakeLivesPanelRect.top + dp(18), hudLabelPaint);
        
        float lifeSize = dp(14); // Match barHeight
        float lifeGap = dp(10);
        float totalLifeW = (lifeSize * 3) + (lifeGap * 2);
        float startLifeX = snakeLivesPanelRect.centerX() - totalLifeW / 2 + lifeSize / 2;
        
        // Vertically centered at same position as progress bar
        float ly = snakeLivesPanelRect.centerY() - dp(2); // Slight adjustment to match visual center
        
        for (int i = 0; i < 3; i++) {
            float lx = startLifeX + i * (lifeSize + lifeGap);
            boolean isFull = i < snakeEngine.getLives();
            
            if (isFull) {
                glowPaint.setColor(Color.WHITE);
                glowPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(lx, ly, lifeSize / 2, glowPaint);
            } else {
                glowPaint.setColor(Color.WHITE);
                glowPaint.setStyle(Paint.Style.STROKE);
                glowPaint.setStrokeWidth(dp(1.2f));
                canvas.drawCircle(lx, ly, lifeSize / 2 - dp(0.5f), glowPaint);
                glowPaint.setStyle(Paint.Style.FILL);
            }
        }

        // Subtitle text (3 LEFT, 2 LEFT...)
        helperPaint.setTextSize(dp(11));
        helperPaint.setTextAlign(Paint.Align.CENTER);
        String livesLeftText = snakeEngine.getLives() + " LEFT";
        // Same bottom offset as Speed Flow panel (bottom - dp(16))
        canvas.drawText(livesLeftText, snakeLivesPanelRect.centerX(), snakeLivesPanelRect.bottom - dp(16), helperPaint);

        if (!useGestures) {
            buttonPaint.setTextSize(dp(22));
            
            // Re-set them locally to ensure exact order: Left, Right, Down, Up
            float btnGap = dp(14);
            float btnW = (snakeLevelBarRect.right - snakeLivesPanelRect.left - btnGap * 3) / 4f;
            float curX = snakeLivesPanelRect.left;
            float ctrlY = leftButton.top; // Use the top from layoutScene
            
            leftButton.set(curX, ctrlY, curX + btnW, ctrlY + btnW);
            curX += btnW + btnGap;
            rightButton.set(curX, ctrlY, curX + btnW, ctrlY + btnW);
            curX += btnW + btnGap;
            dropButton.set(curX, ctrlY, curX + btnW, ctrlY + btnW);
            curX += btnW + btnGap;
            rotateButton.set(curX, ctrlY, curX + btnW, ctrlY + btnW);

            drawButton(canvas, leftButton, "←", 0xCC143456, 0x6636D6FF);
            drawButton(canvas, rightButton, "→", 0xCC143456, 0x6636D6FF);
            drawButton(canvas, dropButton, "↓", 0xCC143456, 0x6636D6FF);
            drawButton(canvas, rotateButton, "↑", 0xCC143456, 0x6636D6FF);
        } else {
            helperPaint.setTextSize(dp(12));
            canvas.drawText("Swipe in any direction to control the snake.",
                    getWidth() * 0.5f, getHeight() - dp(32), helperPaint);
        }

        if (snakeEngine.isPaused()) {
            overlayPaint.setColor(0xBB000000);
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);
            drawGlassCard(canvas, menuModalRect, 0xDD102136, 0x884ECFFF);
            overlayTextPaint.setTextSize(dp(28));
            canvas.drawText("PAUSED", menuModalRect.centerX(), menuModalRect.top + dp(45), overlayTextPaint);
            drawButton(canvas, continueBtnRect, "RESUME", 0xCC1D3952, 0x664ECFFF);
            drawButton(canvas, restartBtnRect, "RESTART", 0xCC4A2834, 0x66FF9A84);
            drawButton(canvas, homeBtnRect, "HOME", 0xCC2A2052, 0x66F47BF5);
        } else if (snakeEngine.isGameOver()) {
            overlayPaint.setColor(0xCC000000);
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);
            drawGlassCard(canvas, menuModalRect, 0xDD250E19, 0x88FF9A84);
            overlayTextPaint.setTextSize(dp(30));
            canvas.drawText("SNAKE COLLAPSE", menuModalRect.centerX(), menuModalRect.top + dp(50), overlayTextPaint);
            
            // Score in Yellow
            hudLabelPaint.setColor(0xFFFFD35A);
            hudLabelPaint.setTextSize(dp(22));
            canvas.drawText("SCORE: " + snakeEngine.getScore(), menuModalRect.centerX(), menuModalRect.top + dp(90), hudLabelPaint);
            hudLabelPaint.setColor(Color.WHITE);
            
            // Adjust buttons for the modal
            float modalBtnW = menuModalRect.width() * 0.8f;
            float modalBtnH = dp(54);
            float modalBtnX = menuModalRect.left + (menuModalRect.width() - modalBtnW) / 2f;
            gameOverRestartBtnRect.set(modalBtnX, menuModalRect.top + dp(120), modalBtnX + modalBtnW, menuModalRect.top + dp(120) + modalBtnH);
            gameOverMenuBtnRect.set(modalBtnX, gameOverRestartBtnRect.bottom + dp(16), modalBtnX + modalBtnW, gameOverRestartBtnRect.bottom + dp(16) + modalBtnH);

            drawButton(canvas, gameOverRestartBtnRect, "RESTART", 0xCC4A2834, 0x66FF9A84);
            drawButton(canvas, gameOverMenuBtnRect, "MAIN MENU", 0xCC2A2052, 0x66F47BF5);
        }
    }

    private boolean contains(RectF rect, float x, float y) {
        return rect.contains(x, y);
    }
    private static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private static int lighten(int color, float factor) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        r += (int) ((255 - r) * factor);
        g += (int) ((255 - g) * factor);
        b += (int) ((255 - b) * factor);
        return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    private static int darken(int color, float factor) {
        int r = (int) (Color.red(color) * (1f - factor));
        int g = (int) (Color.green(color) * (1f - factor));
        int b = (int) (Color.blue(color) * (1f - factor));
        return Color.rgb(Math.max(0, r), Math.max(0, g), Math.max(0, b));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void handleColumnsTouch(float x, float y) {
        if (columnsEngine.isPaused()) {
            if (contains(continueBtnRect, x, y)) {
                columnsEngine.togglePause();
                playSound(soundRotate);
            } else if (contains(restartBtnRect, x, y)) {
                startGame();
                playSound(soundRotate);
            } else if (contains(homeBtnRect, x, y)) {
                inMainMenu = true;
                updateMusic();
                playSound(soundRotate);
            }
            return;
        }

        if (columnsEngine.isGameOver()) {
            if (contains(gameOverRestartBtnRect, x, y)) {
                startGame();
                playSound(soundRotate);
            } else if (contains(gameOverMenuBtnRect, x, y)) {
                inMainMenu = true;
                updateMusic();
                playSound(soundRotate);
            }
            return;
        }

        if (contains(pauseButton, x, y)) {
            columnsEngine.togglePause();
            playSound(soundRotate);
            return;
        }

        if (contains(leftButton, x, y)) {
            if (columnsEngine.move(-1)) playSound(soundRotate);
        } else if (contains(rightButton, x, y)) {
            if (columnsEngine.move(1)) playSound(soundRotate);
        } else if (contains(rotateButton, x, y)) {
            columnsEngine.rotate();
            playSound(soundRotate);
        } else if (contains(dropButton, x, y)) {
            columnsEngine.fastDrop();
            boardShake = 0.5f;
            playSound(soundDrop);
        }
    }

    private void drawColumnsHud(Canvas canvas) {
        drawButton(canvas, pauseButton, "", 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, mainScoreRect, 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, bestScoreRect, 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, nextRect, 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, progressRect, 0xCC143456, 0x6636D6FF);

        // Pause Icon
        float cx = pauseButton.centerX();
        float cy = pauseButton.centerY();
        float pw = dp(4), ph = dp(16), pgap = dp(3);
        buttonPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(cx - pgap - pw, cy - ph/2f, cx - pgap, cy + ph/2f, dp(1), dp(1), buttonPaint);
        canvas.drawRoundRect(cx + pgap, cy - ph/2f, cx + pgap + pw, cy + ph/2f, dp(1), dp(1), buttonPaint);

        // Score & Combo Area (Animated)
        hudLabelPaint.setTextAlign(Paint.Align.CENTER);
        hudValuePaint.setTextAlign(Paint.Align.CENTER);
        
        // 0 anim = centered score, 1 anim = shifted score + combo
        float scoreShiftY = columnsComboAnim * dp(12);
        float scoreValueY = mainScoreRect.centerY() + dp(12) - scoreShiftY;
        float scoreLabelY = mainScoreRect.top + dp(18) - (columnsComboAnim * dp(4));

        canvas.drawText("SCORE", mainScoreRect.centerX(), scoreLabelY, hudLabelPaint);
        drawAutoScaledText(canvas, String.valueOf(columnsEngine.getScore()), mainScoreRect.centerX(), scoreValueY, mainScoreRect.width() - dp(20), hudValuePaint, dp(32));
        
        if (columnsComboAnim > 0.05f) {
            String comboT = "COMBO x" + columnsEngine.getCombo();
            hudLabelPaint.setColor(0xFFFFA35C); // Orange/Gold
            hudLabelPaint.setAlpha((int)(255 * columnsComboAnim));
            canvas.drawText(comboT, mainScoreRect.centerX(), mainScoreRect.bottom - dp(10), hudLabelPaint);
            hudLabelPaint.setAlpha(255);
            hudLabelPaint.setColor(0xFFD8F4FF); // Restore
        }

        // Best
        hudLabelPaint.setTextAlign(Paint.Align.LEFT);
        hudValuePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("BEST", bestScoreRect.left + dp(44), bestScoreRect.top + dp(20), hudLabelPaint);
        drawAutoScaledText(canvas, String.valueOf(bestScoreColumns), bestScoreRect.left + dp(44), bestScoreRect.centerY() + dp(12), bestScoreRect.width() - dp(52), hudValuePaint, dp(18));
        canvas.drawText("🏆", bestScoreRect.left + dp(22), bestScoreRect.centerY() - (buttonPaint.descent() + buttonPaint.ascent()) / 2, buttonPaint);


        // Next Piece
        hudLabelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("NEXT", nextRect.centerX(), nextRect.top + dp(22), hudLabelPaint);
        int[] nextCol = columnsEngine.getNextColumn();
        float nextCellSize = Math.min(nextRect.width() * 0.18f, nextRect.height() * 0.18f);
        float nextY = nextRect.centerY() + dp(12);
        float totalW = nextCellSize * 4;
        float nextXStart = nextRect.centerX() - totalW / 2 + nextCellSize / 2;
        for (int i = 0; i < 4; i++) {
            float cx_i = nextXStart + i * nextCellSize;
            RectF cellR = new RectF(cx_i - nextCellSize*0.42f, nextY - nextCellSize*0.42f, 
                                    cx_i + nextCellSize*0.42f, nextY + nextCellSize*0.42f);
            drawJellyCellAt(canvas, cellR, nextCol[i] - 1, 0, 0, false, true, false);
        }

        // Speed Flow
        hudLabelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("SPEED FLOW", progressRect.centerX(), progressRect.top + dp(22), hudLabelPaint);
        float barHeight = dp(14);
        float barWidth = progressRect.width() * 0.8f;
        RectF bar = new RectF(progressRect.centerX() - barWidth/2, progressRect.centerY() - barHeight/2,
                              progressRect.centerX() + barWidth/2, progressRect.centerY() + barHeight/2);
        
        // Bar background
        glowPaint.setShader(null);
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setColor(0xAA000000); // Darker
        canvas.drawRoundRect(bar, barHeight/2, barHeight/2, glowPaint);

        float progress = (columnsEngine.getTotalCleared() % 32) / 32f;
        RectF fill = new RectF(bar.left, bar.top, bar.left + bar.width() * progress, bar.bottom);
        
        glowPaint.setShader(new LinearGradient(fill.left, fill.top, fill.right, fill.bottom, 
                new int[]{0xFF009AFC, 0xFF1A33E0, 0xFF7912D3, 0xFFF21EE0}, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(fill, barHeight/2, barHeight/2, glowPaint);
        glowPaint.setShader(null);

        helperPaint.setTextSize(dp(11));
        helperPaint.setTextAlign(Paint.Align.CENTER);
        String text = "LEVEL " + columnsEngine.getLevel() + " • " + (32 - columnsEngine.getTotalCleared() % 32) + " CLEARED TO NEXT";
        canvas.drawText(text, progressRect.centerX(), progressRect.bottom - dp(16), helperPaint);

    }

    private void drawColumnsBoard(Canvas canvas, float time) {
        // Board Background
        drawPlayfieldBackgroundAndStroke(canvas, boardRect);

        float cellW = boardRect.width() / ColumnsEngine.COLS;
        float cellH = boardRect.height() / ColumnsEngine.ROWS;

        // Circle grid as requested
        gridPaint.setColor(0x1AFFFFFF);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1f));
        float circleRadius = Math.min(cellW, cellH) * 0.35f;
        for (int r = 0; r < ColumnsEngine.ROWS; r++) {
            for (int c = 0; c < ColumnsEngine.COLS; c++) {
                canvas.drawCircle(boardRect.left + c * cellW + cellW/2, boardRect.top + r * cellH + cellH/2, circleRadius, gridPaint);
            }
        }

        canvas.save();
        canvas.clipRect(boardRect);

        ColumnsEngine.Piece[][] grid = columnsEngine.getGrid();
        for (int r = 0; r < ColumnsEngine.ROWS; r++) {
            for (int c = 0; c < ColumnsEngine.COLS; c++) {
                if (grid[r][c] != null) {
                    RectF rect = new RectF(boardRect.left + c * cellW, boardRect.top + r * cellH, 
                                          boardRect.left + (c+1) * cellW, boardRect.top + (r+1) * cellH);
                    drawJellyCellAt(canvas, rect, grid[r][c].color - 1, 0, 0, false, true, false);
                }
            }
        }

        if (!columnsEngine.isResolving() && !columnsEngine.isGameOver()) {
            int[] column = columnsEngine.getFallingColumn();
            int cx = columnsEngine.getColX();
            // Ghost Piece Calculation
            int ghostR = columnsEngine.getGhostRow();
            if (ghostR != -1) {
                for (int i = 0; i < 4; i++) {
                    int row = ghostR + i;
                    if (row >= 0 && row < ColumnsEngine.ROWS) {
                        RectF rect = new RectF(boardRect.left + cx * cellW, boardRect.top + row * cellH,
                                              boardRect.left + (cx+1) * cellW, boardRect.top + (row+1) * cellH);
                        drawJellyCellAt(canvas, rect, column[i] - 1, 0, 0, false, true, true);
                    }
                }
            }

            // Snap visual Y to grid to match "двигатся по сетке"
            int cy = Math.round(columnsEngine.getColY());
            for (int i = 0; i < 4; i++) {
                int row = cy + i;
                if (row >= 0 && row < ColumnsEngine.ROWS) {
                    RectF rect = new RectF(boardRect.left + cx * cellW, boardRect.top + row * cellH,
                                          boardRect.left + (cx+1) * cellW, boardRect.top + (row+1) * cellH);
                    drawJellyCellAt(canvas, rect, column[i] - 1, 0, 0, true, true, false);
                }
            }
        }
        
        canvas.restore();
    }

    private void drawColumnsOverlay(Canvas canvas) {
        if (!useGestures) {
            buttonPaint.setTextSize(dp(22));
            drawButton(canvas, leftButton, "←", 0xCC143456, 0x6636D6FF);
            drawButton(canvas, rotateButton, "↺", 0xCC2A2052, 0x66F47BF5);
            drawButton(canvas, rightButton, "→", 0xCC143456, 0x6636D6FF);
            drawButton(canvas, dropButton, "↓", 0xCC4A2834, 0x66FF9A84);
        } else {
            helperPaint.setTextSize(dp(12));
            canvas.drawText("Swipe Left/Right to move, Swipe Up to rotate, Down to drop.",
                    getWidth() * 0.5f, getHeight() - dp(32), helperPaint);
        }

        if (columnsEngine.isPaused()) {
            overlayPaint.setColor(0xBB000000);
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);
            drawGlassCard(canvas, menuModalRect, 0xDD102136, 0x884ECFFF);
            overlayTextPaint.setTextSize(dp(28));
            canvas.drawText("PAUSED", menuModalRect.centerX(), menuModalRect.top + dp(45), overlayTextPaint);
            drawButton(canvas, continueBtnRect, "RESUME", 0xCC1D3952, 0x664ECFFF);
            drawButton(canvas, restartBtnRect, "RESTART", 0xCC4A2834, 0x66FF9A84);
            drawButton(canvas, homeBtnRect, "HOME", 0xCC2A2052, 0x66F47BF5);
        } else if (columnsEngine.isGameOver()) {
            overlayPaint.setColor(0xCC000000);
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);
            drawGlassCard(canvas, menuModalRect, 0xDD250E19, 0x88FF9A84);
            overlayTextPaint.setTextSize(dp(30));
            canvas.drawText("COLUMNS OVER", menuModalRect.centerX(), menuModalRect.top + dp(50), overlayTextPaint);
            
            hudLabelPaint.setColor(0xFFFFD35A);
            hudLabelPaint.setTextSize(dp(22));
            hudLabelPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("SCORE: " + columnsEngine.getScore(), menuModalRect.centerX(), menuModalRect.top + dp(90), hudLabelPaint);
            hudLabelPaint.setColor(0xFFD8F4FF);
            
            float mBtnW = menuModalRect.width() * 0.8f;
            float mBtnH = dp(54);
            float mBtnX = menuModalRect.left + (menuModalRect.width() - mBtnW) / 2f;
            gameOverRestartBtnRect.set(mBtnX, menuModalRect.top + dp(120), mBtnX + mBtnW, menuModalRect.top + dp(120) + mBtnH);
            gameOverMenuBtnRect.set(mBtnX, gameOverRestartBtnRect.bottom + dp(16), mBtnX + mBtnW, gameOverRestartBtnRect.bottom + dp(16) + mBtnH);

            drawButton(canvas, gameOverRestartBtnRect, "RESTART", 0xCC4A2834, 0x66FF9A84);
            drawButton(canvas, gameOverMenuBtnRect, "MAIN MENU", 0xCC2A2052, 0x66F47BF5);
        }
    }

    private static final class BackgroundOrb {
        final float x;
        final float y;
        final float speed;
        final float phase;
        float pullOffset;
        float pullVelocity;

        BackgroundOrb(float x, float y, float speed) {
            this.x = x;
            this.y = y;
            this.speed = 0.4f + speed;
            this.phase = speed * 7f;
        }
    }

    private void drawCollapseHud(Canvas canvas) {
        drawButton(canvas, pauseButton, "", 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, bestScoreRect, 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, mainScoreRect, 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, nextRect, 0xC81C3045, 0x66FFE77B);
        drawGlassCard(canvas, progressRect, 0xC81A2940, 0x66FFFFFF);

        // Pause Button symbol
        float cx = pauseButton.centerX();
        float cy = pauseButton.centerY();
        float pw = dp(4);
        float ph = dp(16);
        float pgap = dp(3);
        buttonPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(cx - pgap - pw, cy - ph/2f, cx - pgap, cy + ph/2f, dp(1), dp(1), buttonPaint);
        canvas.drawRoundRect(cx + pgap, cy - ph/2f, cx + pgap + pw, cy + ph/2f, dp(1), dp(1), buttonPaint);

        buttonPaint.setTextSize(dp(20));
        canvas.drawText("🏆", bestScoreRect.left + dp(22), bestScoreRect.centerY() - (buttonPaint.descent() + buttonPaint.ascent()) / 2, buttonPaint);
        
        hudLabelPaint.setTextAlign(Paint.Align.LEFT);
        hudValuePaint.setTextAlign(Paint.Align.LEFT);
        
        hudLabelPaint.setTextSize(dp(11));
        canvas.drawText("BEST", bestScoreRect.left + dp(44), bestScoreRect.top + dp(20), hudLabelPaint);
        drawAutoScaledText(canvas, String.valueOf(bestScoreCollapse), bestScoreRect.left + dp(44), bestScoreRect.centerY() + dp(12), bestScoreRect.width() - dp(52), hudValuePaint, dp(18));
        
        hudLabelPaint.setTextAlign(Paint.Align.CENTER);
        hudValuePaint.setTextAlign(Paint.Align.CENTER);
        
        // Slide animation for combo display
        float slide = collapseComboAnim;
        float scoreLabelY = mainScoreRect.top + dp(30) - slide * dp(8);
        float scoreValueY = mainScoreRect.top + dp(60) - slide * dp(12);

        canvas.drawText("SCORE", mainScoreRect.centerX(), scoreLabelY, hudLabelPaint);
        hudValuePaint.setTextSize(dp(36));
        drawAutoScaledText(canvas, String.valueOf(collapseEngine.getScore()), mainScoreRect.centerX(), scoreValueY, mainScoreRect.width() - dp(20), hudValuePaint, dp(36));

        if (collapseComboAnim > 0.05f) {
            hudLabelPaint.setColor(0xFFFFD35A); // Gold
            hudLabelPaint.setAlpha((int) (collapseComboAnim * 255));
            hudLabelPaint.setTextSize(dp(12));
            canvas.drawText("COMBO x" + collapseEngine.getComboSystem().getComboCount(), mainScoreRect.centerX(), mainScoreRect.bottom - dp(12), hudLabelPaint);
            hudLabelPaint.setAlpha(255);
            hudLabelPaint.setColor(Color.WHITE);
        }

        // Draw next piece preview
        hudLabelPaint.setTextSize(dp(11));
        canvas.drawText("NEXT", nextRect.centerX(), nextRect.top + dp(20), hudLabelPaint);
        
        CollapseEngine.Piece nextPiece = collapseEngine.getNext();
        if (nextPiece != null) {
            float previewCell = Math.min(nextRect.width(), nextRect.height()) / 5.6f;
            int[][] shape = CollapseEngine.getShapeCells(nextPiece.type, 0);

            int minCol = Integer.MAX_VALUE;
            int maxCol = Integer.MIN_VALUE;
            int minRow = Integer.MAX_VALUE;
            int maxRow = Integer.MIN_VALUE;
            for (int[] cell : shape) {
                if (cell[0] < minCol) minCol = cell[0];
                if (cell[0] > maxCol) maxCol = cell[0];
                if (cell[1] < minRow) minRow = cell[1];
                if (cell[1] > maxRow) maxRow = cell[1];
            }

            float shapeWidth = (maxCol - minCol + 1) * previewCell - dp(4);
            float shapeHeight = (maxRow - minRow + 1) * previewCell - dp(4);

            float availableTop = nextRect.top + dp(24);
            float availableHeight = nextRect.bottom - availableTop;

            float startX = nextRect.left + (nextRect.width() - shapeWidth) / 2f - minCol * previewCell;
            float startY = availableTop + (availableHeight - shapeHeight) / 2f - minRow * previewCell;

            for (int[] cell : shape) {
                float x = startX + cell[0] * previewCell;
                float y = startY + cell[1] * previewCell;
                RectF cellR = new RectF(x, y, x + previewCell - dp(4), y + previewCell - dp(4));
                int cIndex = nextPiece.type;
                drawJellyCellAt(canvas, cellR, getCollapseColorIndex(cIndex), 0, 0, true, false, false);
            }
        }

        // Draw progress panel (rising row timer and level indicator)
        hudLabelPaint.setTextSize(dp(11));
        canvas.drawText("PRESSURE", progressRect.centerX(), progressRect.top + dp(20), hudLabelPaint);

        float barHeight = dp(10);
        float barWidth = progressRect.width() - dp(24);
        RectF bar = new RectF(progressRect.centerX() - barWidth/2f, progressRect.centerY() - barHeight/2f + dp(2),
                              progressRect.centerX() + barWidth/2f, progressRect.centerY() + barHeight/2f + dp(2));
        
        float progress = collapseEngine.getRisingRowProgress();
        RectF fill = new RectF(bar.left, bar.top, bar.left + bar.width() * progress, bar.bottom);

        // High-Contrast neon glowing warning bar
        // 1. Dark track background
        overlayPaint.setColor(0x33000000); 
        overlayPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(bar, barHeight/2, barHeight/2, overlayPaint);
        
        // 2. Crisp bright white-gray border to pop out of the dark panel
        overlayPaint.setColor(0x55FFFFFF); 
        overlayPaint.setStyle(Paint.Style.STROKE);
        overlayPaint.setStrokeWidth(dp(1f));
        canvas.drawRoundRect(bar, barHeight/2, barHeight/2, overlayPaint);
        overlayPaint.setStyle(Paint.Style.FILL); // restore

        // 3. 4-color gradient progress bar
        if (progress > 0.01f) {
            glowPaint.setStyle(Paint.Style.FILL);
            glowPaint.setAlpha(255);
            glowPaint.setShader(new LinearGradient(fill.left, fill.top, fill.right, fill.bottom,
                    new int[]{0xFF009AFC, 0xFF1A33E0, 0xFF7912D3, 0xFFF21EE0}, null, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(fill, barHeight/2, barHeight/2, glowPaint);
            glowPaint.setShader(null);
        }
        
        helperPaint.setTextSize(dp(11));
        String text = "LEVEL " + collapseEngine.getLevel() + " • RISE " + String.format("%.1fs", collapseEngine.getRisingRowInterval() * (1f - progress));
        canvas.drawText(text, progressRect.centerX(), progressRect.bottom - dp(16), helperPaint);
    }

    private void drawCollapseBoard(Canvas canvas, float time) {
        // Radial glow shader for premium aesthetics
        glowPaint.setShader(new RadialGradient(
                boardRect.centerX(), boardRect.top + boardRect.height() * 0.28f, boardRect.width() * 0.7f,
                0x3047C5FF, 0x00000000, Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(boardGlowRect, dp(28), dp(28), glowPaint);
        glowPaint.setShader(null);

        drawPlayfieldBackgroundAndStroke(canvas, boardRect);

        float cell = boardRect.width() / CollapseEngine.COLS;
        float rowHeight = boardRect.height() / CollapseEngine.ROWS;

        // Draw light neon glass-grid cells
        gridPaint.setColor(0x18FFFFFF);
        blockStrokePaint.setColor(0x44FFFFFF);
        for (int row = 0; row < CollapseEngine.ROWS; row++) {
            for (int col = 0; col < CollapseEngine.COLS; col++) {
                RectF rect = cellRect(col, row, cell, rowHeight);
                canvas.drawRoundRect(rect, dp(10), dp(10), gridPaint);
            }
        }

        // Draw ghost pieces
        List<int[]> ghost = collapseEngine.getGhostCells();
        CollapseEngine.Piece activePiece = collapseEngine.getActive();
        int ghostColorIdx = (activePiece != null) ? getCollapseColorIndex(activePiece.type) : 0;
        for (int[] cellPos : ghost) {
            if (cellPos[1] >= 0) {
                drawGhostCell(canvas, cellPos[0], cellPos[1], ghostColorIdx, cell, rowHeight);
            }
        }

        // Draw settled blocks
        for (int row = 0; row < CollapseEngine.ROWS; row++) {
            for (int col = 0; col < CollapseEngine.COLS; col++) {
                int value = collapseEngine.getCell(row, col);
                if (value != 0) {
                    drawJellyCell(canvas, col, row, getCollapseColorIndex(value - 1), cell, rowHeight,
                            0.05f * (float) Math.sin(time * 2.2f + row * 0.4f + col * 0.7f),
                            0.08f * (float) Math.cos(time * 2.8f + col * 0.3f), false, false);
                }
            }
        }

        // Draw active floating piece
        CollapseEngine.Piece active = collapseEngine.getActive();
        if (active != null) {
            List<int[]> activeCells = collapseEngine.getActiveCells();
            for (int i = 0; i < activeCells.size(); i++) {
                int[] cellPos = activeCells.get(i);
                if (cellPos[1] >= 0) {
                    float jelly = 0.14f * (float) Math.sin(time * 7f + i * 1.1f + collapseEngine.getProgressToFall() * 5f);
                    float sway = 0.16f * (float) Math.cos(time * 4.3f + i * 0.8f);
                    drawJellyCell(canvas, cellPos[0], cellPos[1], getCollapseColorIndex(active.type), cell, rowHeight, jelly, sway, true, false);
                }
            }
        }
    }

    private void drawCollapseOverlay(Canvas canvas) {
        if (!useGestures) {
            buttonPaint.setTextSize(dp(26));
            drawButton(canvas, leftButton, "←", 0xCC143456, 0x6636D6FF);
            drawButton(canvas, rotateButton, "⟲", 0xCC2A2052, 0x66F47BF5);
            drawButton(canvas, rightButton, "→", 0xCC143456, 0x6636D6FF);
            drawButton(canvas, dropButton, "↓", 0xCC143456, 0x6636D6FF);

            helperPaint.setTextSize(dp(11));
            canvas.drawText("Irregular tetrominoes fall down. Bottom rows rise under pressure.",
                    getWidth() * 0.5f, dropButton.bottom + dp(22), helperPaint);
        } else {
            helperPaint.setTextSize(dp(12));
            canvas.drawText("Swipe Left/Right to move, Swipe Up/Down to Drop, Tap to rotate.",
                    getWidth() * 0.5f, getHeight() - dp(32), helperPaint);
        }

        if (collapseEngine.isPaused()) {
            overlayPaint.setColor(0xBB000000);
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

            drawGlassCard(canvas, menuModalRect, 0xDD102136, 0x884ECFFF);
            
            overlayTextPaint.setTextSize(dp(28));
            canvas.drawText("MENU", menuModalRect.centerX(), menuModalRect.top + dp(45), overlayTextPaint);
            
            buttonPaint.setTextSize(dp(20));
            drawButton(canvas, continueBtnRect, "RESUME", 0xCC1D3952, 0x664ECFFF);
            drawButton(canvas, restartBtnRect, "RESTART", 0xCC4A2834, 0x66FF9A84);
            drawButton(canvas, homeBtnRect, "HOME", 0xCC2A2052, 0x66F47BF5);
        } else if (collapseEngine.isGameOver()) {
            overlayPaint.setColor(0xCC000000); // Full screen darkening
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

            drawGlassCard(canvas, menuModalRect, 0xDD250E19, 0x88FF9A84);
            overlayTextPaint.setTextSize(dp(30));
            canvas.drawText("COLLAPSE OVER", menuModalRect.centerX(), menuModalRect.top + dp(50), overlayTextPaint);
            
            // Score in Yellow
            hudLabelPaint.setColor(0xFFFFD35A);
            hudLabelPaint.setTextSize(dp(22));
            canvas.drawText("SCORE: " + collapseEngine.getScore(), menuModalRect.centerX(), menuModalRect.top + dp(90), hudLabelPaint);
            hudLabelPaint.setColor(Color.WHITE);
            
            buttonPaint.setTextSize(dp(20));
            // Reuse gameOver rects but centered in modal
            float mBtnW = menuModalRect.width() * 0.8f;
            float mBtnH = dp(54);
            float mBtnX = menuModalRect.left + (menuModalRect.width() - mBtnW) / 2f;
            gameOverRestartBtnRect.set(mBtnX, menuModalRect.top + dp(120), mBtnX + mBtnW, menuModalRect.top + dp(120) + mBtnH);
            gameOverMenuBtnRect.set(mBtnX, gameOverRestartBtnRect.bottom + dp(16), mBtnX + mBtnW, gameOverRestartBtnRect.bottom + dp(16) + mBtnH);

            drawButton(canvas, gameOverRestartBtnRect, "RESTART", 0xCC4A2834, 0x66FF9A84);
            drawButton(canvas, gameOverMenuBtnRect, "MAIN MENU", 0xCC2A2052, 0x66F47BF5);
        }
    }

    private void drawBreakerHud(Canvas canvas) {
        drawButton(canvas, pauseButton, "", 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, bestScoreRect, 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, mainScoreRect, 0xCC143456, 0x6636D6FF);
        drawGlassCard(canvas, nextRect, 0xC81C3045, 0x66FFE77B);
        drawGlassCard(canvas, progressRect, 0xC81A2940, 0x66FFFFFF);

        // Pause Button symbol
        float cx = pauseButton.centerX();
        float cy = pauseButton.centerY();
        float pw = dp(4);
        float ph = dp(16);
        float pgap = dp(3);
        buttonPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(cx - pgap - pw, cy - ph/2f, cx - pgap, cy + ph/2f, dp(1), dp(1), buttonPaint);
        canvas.drawRoundRect(cx + pgap, cy - ph/2f, cx + pgap + pw, cy + ph/2f, dp(1), dp(1), buttonPaint);

        buttonPaint.setTextSize(dp(20));
        canvas.drawText("🏆", bestScoreRect.left + dp(22), bestScoreRect.centerY() - (buttonPaint.descent() + buttonPaint.ascent()) / 2, buttonPaint);
        
        hudLabelPaint.setTextAlign(Paint.Align.LEFT);
        hudValuePaint.setTextAlign(Paint.Align.LEFT);
        
        hudLabelPaint.setTextSize(dp(11));
        canvas.drawText("BEST", bestScoreRect.left + dp(44), bestScoreRect.top + dp(20), hudLabelPaint);
        drawAutoScaledText(canvas, String.valueOf(bestScoreBreaker), bestScoreRect.left + dp(44), bestScoreRect.centerY() + dp(12), bestScoreRect.width() - dp(52), hudValuePaint, dp(18));
        
        hudLabelPaint.setTextAlign(Paint.Align.CENTER);
        hudValuePaint.setTextAlign(Paint.Align.CENTER);
        
        // Score display
        float scoreLabelY = mainScoreRect.top + dp(30);
        float scoreValueY = mainScoreRect.top + dp(60);

        canvas.drawText("SCORE", mainScoreRect.centerX(), scoreLabelY, hudLabelPaint);
        hudValuePaint.setTextSize(dp(36));
        drawAutoScaledText(canvas, String.valueOf(breakerEngine.getScore()), mainScoreRect.centerX(), scoreValueY, mainScoreRect.width() - dp(20), hudValuePaint, dp(36));

        // Combo display
        if (breakerEngine.getComboDisplayTimer() > 0.05f) {
            hudLabelPaint.setColor(0xFFFFD35A); // Gold
            hudLabelPaint.setAlpha((int) (breakerEngine.getComboDisplayTimer() / 1.6f * 255));
            hudLabelPaint.setTextSize(dp(12));
            canvas.drawText("COMBO x" + breakerEngine.getComboCount(), mainScoreRect.centerX(), mainScoreRect.bottom - dp(12), hudLabelPaint);
            hudLabelPaint.setAlpha(255);
            hudLabelPaint.setColor(Color.WHITE);
        }

        // Draw lives preview in nextRect
        hudLabelPaint.setTextSize(dp(11));
        canvas.drawText("LIVES", nextRect.centerX(), nextRect.top + dp(20), hudLabelPaint);
        
        int lives = breakerEngine.getLives();
        float lifeRadius = dp(8);
        float spacing = dp(22);
        float startX = nextRect.centerX() - spacing;
        float centerY = nextRect.centerY() + dp(8);
        for (int i = 0; i < 3; i++) {
            float lx = startX + i * spacing;
            if (i < lives) {
                hudLabelPaint.setColor(0xFFFF4E7E);
                hudLabelPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(lx, centerY, lifeRadius, hudLabelPaint);
                hudLabelPaint.setColor(Color.WHITE);
                canvas.drawCircle(lx - lifeRadius*0.3f, centerY - lifeRadius*0.3f, lifeRadius*0.25f, hudLabelPaint);
            } else {
                hudLabelPaint.setColor(0x44FFFFFF);
                hudLabelPaint.setStyle(Paint.Style.STROKE);
                hudLabelPaint.setStrokeWidth(dp(1.5f));
                canvas.drawCircle(lx, centerY, lifeRadius, hudLabelPaint);
            }
        }
        hudLabelPaint.setStyle(Paint.Style.FILL); // Restore
        hudLabelPaint.setColor(Color.WHITE);

        // Draw pressure/falling rows countdown bar
        hudLabelPaint.setTextSize(dp(11));
        canvas.drawText("PRESSURE", progressRect.centerX(), progressRect.top + dp(20), hudLabelPaint);

        float barHeight = dp(10);
        float barWidth = progressRect.width() - dp(24);
        RectF bar = new RectF(progressRect.centerX() - barWidth/2f, progressRect.centerY() - barHeight/2f + dp(2),
                              progressRect.centerX() + barWidth/2f, progressRect.centerY() + barHeight/2f + dp(2));
        
        float progress = breakerEngine.getFallingRowProgress();
        RectF fill = new RectF(bar.left, bar.top, bar.left + bar.width() * progress, bar.bottom);

        overlayPaint.setColor(0x33000000); 
        overlayPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(bar, barHeight/2, barHeight/2, overlayPaint);
        
        overlayPaint.setColor(0x55FFFFFF); 
        overlayPaint.setStyle(Paint.Style.STROKE);
        overlayPaint.setStrokeWidth(dp(1f));
        canvas.drawRoundRect(bar, barHeight/2, barHeight/2, overlayPaint);
        overlayPaint.setStyle(Paint.Style.FILL); // restore

        if (progress > 0.01f) {
            glowPaint.setStyle(Paint.Style.FILL);
            glowPaint.setAlpha(255);
            glowPaint.setShader(new LinearGradient(fill.left, fill.top, fill.right, fill.bottom,
                    new int[]{0xFF009AFC, 0xFF1A33E0, 0xFF7912D3, 0xFFF21EE0}, null, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(fill, barHeight/2, barHeight/2, glowPaint);
            glowPaint.setShader(null);
        }
        
        helperPaint.setTextSize(dp(11));
        String text = "LEVEL " + breakerEngine.getLevel() + " • SPEED x" + String.format("%.2f", breakerEngine.getBallSpeedMultiplier()) + " • ROWS " + String.format("%.1fs", breakerEngine.getFallingRowInterval() * (1f - progress));
        canvas.drawText(text, progressRect.centerX(), progressRect.bottom - dp(16), helperPaint);
    }

    private void drawBreakerBoard(Canvas canvas, float time) {
        // Radial background glow
        glowPaint.setShader(new RadialGradient(
                boardRect.centerX(), boardRect.top + boardRect.height() * 0.28f, boardRect.width() * 0.7f,
                0x3047C5FF, 0x00000000, Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(boardGlowRect, dp(28), dp(28), glowPaint);
        glowPaint.setShader(null);

        drawPlayfieldBackgroundAndStroke(canvas, boardRect);

        float cell = boardRect.width() / BreakerEngine.COLS;
        float rowHeight = boardRect.height() / BreakerEngine.ROWS;

        // Draw light neon grid cells
        gridPaint.setColor(0x12FFFFFF);
        for (int row = 0; row < BreakerEngine.ROWS; row++) {
            for (int col = 0; col < BreakerEngine.COLS; col++) {
                RectF rect = cellRect(col, row, cell, rowHeight);
                canvas.drawRoundRect(rect, dp(10), dp(10), gridPaint);
            }
        }

        // Draw pulsing Danger Zone background overlay
        float pulse = (float) Math.sin(time * 6.0f) * 0.5f + 0.5f;
        int dangerColor = withAlpha(0xFFFF3B30, (int) (20 + 25 * pulse));
        overlayPaint.setColor(dangerColor);
        overlayPaint.setStyle(Paint.Style.FILL);
        RectF dangerZoneRect = new RectF(boardRect.left, boardRect.top + BreakerEngine.DANGER_ZONE_START_ROW * rowHeight, boardRect.right, boardRect.bottom);
        canvas.drawRoundRect(dangerZoneRect, dp(12), dp(12), overlayPaint);

        // Danger Zone line
        overlayPaint.setColor(0xFFFF3B30);
        overlayPaint.setStyle(Paint.Style.STROKE);
        overlayPaint.setStrokeWidth(dp(2.0f));
        canvas.drawLine(boardRect.left, boardRect.top + BreakerEngine.DANGER_ZONE_START_ROW * rowHeight, boardRect.right, boardRect.top + BreakerEngine.DANGER_ZONE_START_ROW * rowHeight, overlayPaint);
        overlayPaint.setStyle(Paint.Style.FILL); // restore

        // Danger Zone text
        helperPaint.setColor(0xFFFF3B30);
        helperPaint.setAlpha((int) (40 + 30 * pulse));
        helperPaint.setTextSize(dp(10));
        helperPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("⚠️ DANGER ZONE ⚠️", boardRect.centerX(), boardRect.top + 18.7f * rowHeight, helperPaint);
        helperPaint.setColor(Color.WHITE);
        helperPaint.setAlpha(255);

        // Draw blocks/bricks
        for (int row = 0; row < BreakerEngine.ROWS; row++) {
            for (int col = 0; col < BreakerEngine.COLS; col++) {
                int value = breakerEngine.getCell(row, col);
                if (value == 0) continue;

                if (value >= 1 && value <= 7) { // Standard Brick
                    drawJellyCell(canvas, col, row, value - 1, cell, rowHeight,
                            0.05f * (float) Math.sin(time * 2.2f + row * 0.4f + col * 0.7f),
                            0.08f * (float) Math.cos(time * 2.8f + col * 0.3f), false, false);
                } else if (value == 8) { // Durable Brick
                    int hp = breakerEngine.getBrickHP(row, col);
                    RectF cellR = cellRect(col, row, cell, rowHeight);
                    blockPaint.setShader(new LinearGradient(cellR.left, cellR.top, cellR.right, cellR.bottom,
                                         0xFFBDC3C7, 0xFF2C3E50, Shader.TileMode.CLAMP));
                    canvas.drawRoundRect(cellR, dp(6), dp(6), blockPaint);
                    blockPaint.setShader(null);
                    
                    blockStrokePaint.setColor(0x88FFFFFF);
                    blockStrokePaint.setStrokeWidth(dp(1.0f));
                    blockStrokePaint.setStyle(Paint.Style.STROKE);
                    canvas.drawRoundRect(cellR, dp(6), dp(6), blockStrokePaint);
                    blockStrokePaint.setStyle(Paint.Style.FILL);
                    
                    if (hp <= 2) {
                        blockStrokePaint.setColor(0xCC000000);
                        blockStrokePaint.setStrokeWidth(dp(1.5f));
                        blockStrokePaint.setStyle(Paint.Style.STROKE);
                        canvas.drawLine(cellR.left + cellR.width()*0.2f, cellR.top + cellR.height()*0.3f, 
                                        cellR.left + cellR.width()*0.8f, cellR.top + cellR.height()*0.7f, blockStrokePaint);
                        if (hp == 1) {
                            canvas.drawLine(cellR.left + cellR.width()*0.7f, cellR.top + cellR.height()*0.2f, 
                                            cellR.left + cellR.width()*0.3f, cellR.top + cellR.height()*0.8f, blockStrokePaint);
                        }
                        blockStrokePaint.setStyle(Paint.Style.FILL);
                    }
                } else if (value == 9) { // PLUS Block
                    RectF cellR = cellRect(col, row, cell, rowHeight);
                    blockPaint.setShader(new LinearGradient(cellR.left, cellR.top, cellR.right, cellR.bottom,
                                         0xFFFFE77B, 0xFF4ECFFF, Shader.TileMode.CLAMP));
                    canvas.drawRoundRect(cellR, dp(8), dp(8), blockPaint);
                    blockPaint.setShader(null);
                    
                    glowPaint.setStyle(Paint.Style.STROKE);
                    glowPaint.setColor(0x884ECFFF);
                    glowPaint.setStrokeWidth(dp(2.0f + 1.5f * pulse));
                    canvas.drawRoundRect(cellR, dp(8), dp(8), glowPaint);
                    glowPaint.setStyle(Paint.Style.FILL);
                    
                    overlayTextPaint.setColor(0xFF0A1526);
                    overlayTextPaint.setTextSize(cellR.height() * 0.8f);
                    overlayTextPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("+", cellR.centerX(), cellR.centerY() - (overlayTextPaint.descent() + overlayTextPaint.ascent()) / 2, overlayTextPaint);
                    overlayTextPaint.setColor(Color.WHITE);
                } else if (value == 10) { // MINUS Block
                    RectF cellR = cellRect(col, row, cell, rowHeight);
                    blockPaint.setShader(new LinearGradient(cellR.left, cellR.top, cellR.right, cellR.bottom,
                                         0xFFFF4E50, 0xFFF9D423, Shader.TileMode.CLAMP));
                    canvas.drawRoundRect(cellR, dp(8), dp(8), blockPaint);
                    blockPaint.setShader(null);
                    
                    glowPaint.setStyle(Paint.Style.STROKE);
                    glowPaint.setColor(0x88FF4E50);
                    glowPaint.setStrokeWidth(dp(2.0f + 1.5f * pulse));
                    canvas.drawRoundRect(cellR, dp(8), dp(8), glowPaint);
                    glowPaint.setStyle(Paint.Style.FILL);
                    
                    overlayTextPaint.setColor(0xFF0A1526);
                    overlayTextPaint.setTextSize(cellR.height() * 0.8f);
                    overlayTextPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("-", cellR.centerX(), cellR.centerY() - (overlayTextPaint.descent() + overlayTextPaint.ascent()) / 2, overlayTextPaint);
                    overlayTextPaint.setColor(Color.WHITE);
                }
            }
        }

        // Draw Paddle
        float paddlePxX = boardRect.left + (breakerEngine.getPaddleX() / BreakerEngine.COLS) * boardRect.width();
        float paddlePxY = boardRect.top + (breakerEngine.getPaddleY() / BreakerEngine.ROWS) * boardRect.height() + rowHeight * 0.25f;
        float paddlePxW = (breakerEngine.getPaddleWidth() / BreakerEngine.COLS) * boardRect.width();
        float paddlePxH = rowHeight * 0.45f;

        RectF paddleRect = new RectF(paddlePxX - paddlePxW/2f, paddlePxY - paddlePxH/2f,
                                     paddlePxX + paddlePxW/2f, paddlePxY + paddlePxH/2f);

        blockPaint.setShader(new LinearGradient(paddleRect.left, paddleRect.top, paddleRect.right, paddleRect.top,
                             0xFF4ECFFF, 0xFFF47BF5, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(paddleRect, paddlePxH/2f, paddlePxH/2f, blockPaint);
        blockPaint.setShader(null);

        blockStrokePaint.setColor(0xBBFFFFFF);
        blockStrokePaint.setStrokeWidth(dp(1.2f));
        blockStrokePaint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(paddleRect, paddlePxH/2f, paddlePxH/2f, blockStrokePaint);
        blockStrokePaint.setStyle(Paint.Style.FILL);

        // Draw Balls (with trail and glow)
        float ballRadiusInGrid = 0.22f;
        float ballPxR = (ballRadiusInGrid / BreakerEngine.COLS) * boardRect.width();

        for (BreakerEngine.Ball ball : breakerEngine.getActiveBalls()) {
            List<float[]> trail = ball.trail;
            for (int t = trail.size() - 1; t >= 0; t--) {
                float[] pos = trail.get(t);
                float tx = boardRect.left + (pos[0] / BreakerEngine.COLS) * boardRect.width();
                float ty = boardRect.top + (pos[1] / BreakerEngine.ROWS) * boardRect.height();
                float alphaPct = 1f - (float) (t + 1) / (trail.size() + 1);
                
                int trailColor = Color.HSVToColor((int) (alphaPct * 120), new float[]{ball.hueOffset % 360f, 0.8f, 0.95f});
                blockPaint.setColor(trailColor);
                canvas.drawCircle(tx, ty, ballPxR * (0.5f + 0.5f * alphaPct), blockPaint);
            }

            float bx = boardRect.left + (ball.x / BreakerEngine.COLS) * boardRect.width();
            float by = boardRect.top + (ball.y / BreakerEngine.ROWS) * boardRect.height();
            
            int ballColor = Color.HSVToColor(255, new float[]{ball.hueOffset % 360f, 0.85f, 1.0f});
            blockPaint.setColor(ballColor);
            canvas.drawCircle(bx, by, ballPxR, blockPaint);
            
            glowPaint.setColor(withAlpha(ballColor, 100));
            canvas.drawCircle(bx, by, ballPxR * 1.5f, glowPaint);
            
            blockPaint.setColor(Color.WHITE);
            canvas.drawCircle(bx - ballPxR * 0.25f, by - ballPxR * 0.25f, ballPxR * 0.35f, blockPaint);
        }

        // Draw Explosions
        for (BreakerEngine.Explosion exp : breakerEngine.getActiveExplosions()) {
            float ex = boardRect.left + (exp.col / BreakerEngine.COLS) * boardRect.width();
            float ey = boardRect.top + (exp.row / BreakerEngine.ROWS) * boardRect.height();
            
            float progress = exp.age / exp.maxAge;
            float radius = cell * 2.2f * (0.3f + 0.7f * progress);
            int alpha = (int) ((1f - progress) * 220);
            
            RadialGradient grad = new RadialGradient(ex, ey, radius,
                    new int[]{withAlpha(0xFFFFE359, alpha), withAlpha(0xFFFF5E3A, alpha), 0x00000000},
                    new float[]{0.0f, 0.6f, 1.0f}, Shader.TileMode.CLAMP);
            glowPaint.setShader(grad);
            canvas.drawCircle(ex, ey, radius, glowPaint);
            glowPaint.setShader(null);
        }
    }

    private void drawBreakerOverlay(Canvas canvas) {
        buttonPaint.setTextSize(dp(26));
        drawButton(canvas, leftButton, "←", 0xCC143456, 0x6636D6FF);
        drawButton(canvas, rotateButton, "↑", 0xCC2A2052, 0x66F47BF5);
        drawButton(canvas, rightButton, "→", 0xCC143456, 0x6636D6FF);
        drawButton(canvas, dropButton, "↓", 0xCC143456, 0x6636D6FF);

        helperPaint.setTextSize(dp(11));
        canvas.drawText("Drag screen or use ← → to move. Use ↑ to speed up, ↓ to speed down.",
                getWidth() * 0.5f, dropButton.bottom + dp(22), helperPaint);

        if (breakerEngine.isPaused()) {
            overlayPaint.setColor(0xBB000000);
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

            drawGlassCard(canvas, menuModalRect, 0xDD102136, 0x884ECFFF);
            
            overlayTextPaint.setTextSize(dp(28));
            canvas.drawText("MENU", menuModalRect.centerX(), menuModalRect.top + dp(45), overlayTextPaint);
            
            buttonPaint.setTextSize(dp(20));
            drawButton(canvas, continueBtnRect, "RESUME", 0xCC1D3952, 0x664ECFFF);
            drawButton(canvas, restartBtnRect, "RESTART", 0xCC4A2834, 0x66FF9A84);
            drawButton(canvas, homeBtnRect, "HOME", 0xCC2A2052, 0x66F47BF5);
        } else if (breakerEngine.isGameOver()) {
            overlayPaint.setColor(0xCC000000);
            canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

            drawGlassCard(canvas, menuModalRect, 0xDD250E19, 0x88FF9A84);
            overlayTextPaint.setTextSize(dp(30));
            canvas.drawText("BREAKER OVER", menuModalRect.centerX(), menuModalRect.top + dp(50), overlayTextPaint);
            
            hudLabelPaint.setColor(0xFFFFD35A);
            hudLabelPaint.setTextSize(dp(22));
            canvas.drawText("SCORE: " + breakerEngine.getScore(), menuModalRect.centerX(), menuModalRect.top + dp(90), hudLabelPaint);
            hudLabelPaint.setColor(Color.WHITE);
            
            buttonPaint.setTextSize(dp(20));
            float mBtnW = menuModalRect.width() * 0.8f;
            float mBtnH = dp(54);
            float mBtnX = menuModalRect.left + (menuModalRect.width() - mBtnW) / 2f;
            gameOverRestartBtnRect.set(mBtnX, menuModalRect.top + dp(120), mBtnX + mBtnW, menuModalRect.top + dp(120) + mBtnH);
            gameOverMenuBtnRect.set(mBtnX, gameOverRestartBtnRect.bottom + dp(16), mBtnX + mBtnW, gameOverRestartBtnRect.bottom + dp(16) + mBtnH);

            drawButton(canvas, gameOverRestartBtnRect, "RESTART", 0xCC4A2834, 0x66FF9A84);
            drawButton(canvas, gameOverMenuBtnRect, "MAIN MENU", 0xCC2A2052, 0x66F47BF5);
        }
    }

    private void updateBreakerSimulation(float delta) {
        if (!breakerEngine.isPaused() && !breakerEngine.isGameOver()) {
            if (isLeftButtonPressed) {
                breakerEngine.movePaddleLeft(delta);
            } else if (isRightButtonPressed) {
                breakerEngine.movePaddleRight(delta);
            }
        }
        breakerEngine.update(delta);

        // Sound cues
        if (breakerEngine.checkBounceSound()) {
            playSound(soundRotate);
        }
        if (breakerEngine.checkDestroySound()) {
            playSound(soundDrop);
        }
        if (breakerEngine.checkPowerupSound()) {
            playSound(soundRotate);
        }
        if (breakerEngine.checkGameOverSound()) {
            playSound(soundGameOver);
        }

        // Particle Bursts for falling row line bursts
        float cell = boardRect.width() / BreakerEngine.COLS;
        for (BreakerEngine.LineBurst burst : breakerEngine.getLineBursts()) {
            if (!burst.emitted) {
                burst.emitted = true;
                emitLineBurst(burst.row, burst.colors, cell);
                if (burst.catastrophic) {
                    boardShake = Math.max(boardShake, 0.7f);
                } else {
                    boardShake = Math.max(boardShake, 0.25f);
                }
            }
        }

        // Save best score
        int score = breakerEngine.getScore();
        if (score > bestScoreBreaker) {
            bestScoreBreaker = score;
            getContext().getSharedPreferences("BrickSpaceNeoPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("BEST_SCORE_BREAKER", bestScoreBreaker)
                    .apply();
        }
    }

    private void handleBreakerTouch(float x, float y, int action) {
        if (breakerEngine.isPaused()) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (contains(continueBtnRect, x, y)) {
                    breakerEngine.togglePause();
                    playSound(soundRotate);
                } else if (contains(restartBtnRect, x, y)) {
                    startGame();
                    playSound(soundRotate);
                } else if (contains(homeBtnRect, x, y)) {
                    inMainMenu = true;
                    breakerEngine.togglePause();
                    updateMusic();
                    playSound(soundRotate);
                }
            }
            return;
        }

        if (breakerEngine.isGameOver()) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (contains(gameOverRestartBtnRect, x, y)) {
                    startGame();
                    playSound(soundRotate);
                } else if (contains(gameOverMenuBtnRect, x, y)) {
                    inMainMenu = true;
                    updateMusic();
                    playSound(soundRotate);
                }
            }
            return;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            if (contains(pauseButton, x, y)) {
                breakerEngine.togglePause();
                playSound(soundRotate);
                return;
            }

            if (contains(leftButton, x, y)) {
                isLeftButtonPressed = true;
                return;
            }
            if (contains(rightButton, x, y)) {
                isRightButtonPressed = true;
                return;
            }
            if (contains(rotateButton, x, y)) { // UP speed up
                breakerEngine.increaseSpeed();
                playSound(soundRotate);
                return;
            }
            if (contains(dropButton, x, y)) { // DOWN slow down
                breakerEngine.decreaseSpeed();
                playSound(soundRotate);
                return;
            }

            // Drag touch on board/bottom zone
            if (y > boardRect.top && y < boardRect.bottom + dp(40)) {
                float touchCol = ((x - boardRect.left) / boardRect.width()) * BreakerEngine.COLS;
                breakerEngine.setTargetPaddleX(touchCol);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (!isLeftButtonPressed && !isRightButtonPressed) {
                if (y > boardRect.top && y < boardRect.bottom + dp(40)) {
                    float touchCol = ((x - boardRect.left) / boardRect.width()) * BreakerEngine.COLS;
                    breakerEngine.setTargetPaddleX(touchCol);
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            isLeftButtonPressed = false;
            isRightButtonPressed = false;
        }
    }

    private static final class JellyParticle {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float age;
        float life;
        float phase;
        float rotation;
        float spin;
        float gravity;
        float drag;
        float bounce;
        float floorFriction;
        int colorIndex;
    }

    private static final class FloatingShape {
        float x;
        float y;
        float vx;
        float vy;
        float rotation;
        float spin;
        int type;
        int colorIndex;
        float sizeScale;
    }
}
