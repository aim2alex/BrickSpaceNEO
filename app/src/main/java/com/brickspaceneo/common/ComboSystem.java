package com.brickspaceneo.common;

public class ComboSystem {
    private int comboCount = 0;
    private int currentMultiplier = 1;

    public void reset() {
        comboCount = 0;
        currentMultiplier = 1;
    }

    public int processLinesCleared(int linesCleared) {
        if (linesCleared > 0) {
            comboCount++;

            if (comboCount <= 1) {
                currentMultiplier = 1;
            } else {
                currentMultiplier = Math.min(10, comboCount); // Optional max x10
            }

            int baseScore = getBaseScore(linesCleared);
            int totalScore = baseScore * currentMultiplier;
            
            // Linear bonus
            int comboBonus = comboCount * 50;

            return totalScore + comboBonus;
        } else {
            comboCount = 0;
            currentMultiplier = 1;
            return 0;
        }
    }

    public int getComboCount() {
        return comboCount;
    }

    public int getCurrentMultiplier() {
        return currentMultiplier;
    }

    private int getBaseScore(int linesCleared) {
        if (linesCleared >= 5) {
            return 800 + (linesCleared - 4) * 300;
        }
        switch (linesCleared) {
            case 1: return 100;
            case 2: return 300;
            case 3: return 500;
            case 4: return 800;
            default: return 0;
        }
    }
}
