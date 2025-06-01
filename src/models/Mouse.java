package models;

import maze.Maze;
import java.util.*;

public class Mouse {
    private int x, y;
    private int id;
    private String symbol;
    private Maze maze;
    private Set<String> visitedPositions = new HashSet<>();
    private Stack<int[]> pathStack = new Stack<>();
    private boolean hasReachedEnd = false;
    private Random random = new Random();

    public Mouse(int id, Maze maze) {
        this.id = id;
        this.maze = maze;
        // Símbolos diferentes para cada rato
        String[] symbols = {"@", "♦", "♣", "♠", "♥", "◆", "◇", "★", "☆", "●"};
        this.symbol = symbols[id % symbols.length];
        setRandomPosition();
    }

    /**
     * Define uma posição inicial aleatória para este rato
     */
    private void setRandomPosition() {
        List<int[]> validPositions = new ArrayList<>();

        // Encontra todas as posições válidas
        for (int row = 0; row < maze.getHeight(); row++) {
            for (int col = 0; col < maze.getWidth(); col++) {
                if (maze.isValidPosition(col, row) && !maze.isEndPosition(col, row)) {
                    validPositions.add(new int[]{col, row});
                }
            }
        }

        if (!validPositions.isEmpty()) {
            int randomIndex = random.nextInt(validPositions.size());
            int[] position = validPositions.get(randomIndex);
            this.x = position[0];
            this.y = position[1];
            System.out.println("🐭 Rato " + id + " (" + symbol + ") apareceu em: (" + x + ", " + y + ")");
        } else {
            this.x = 1;
            this.y = 0;
            System.out.println("⚠️ Rato " + id + " usando posição padrão");
        }
    }

    /**
     * Move o rato um passo em direção ao objetivo
     */
    public synchronized boolean move() {
        if (hasReachedEnd) return false;

        // Verifica se chegou ao destino
        if (x == maze.getEndX() && y == maze.getEndY()) {
            hasReachedEnd = true;
            System.out.println("🎉 RATO " + id + " (" + symbol + ") CHEGOU AO DESTINO!");
            return true;
        }

        String currentPos = x + "," + y;

        // Adiciona posição atual ao histórico
        if (!visitedPositions.contains(currentPos)) {
            visitedPositions.add(currentPos);
            pathStack.push(new int[]{x, y});
        }

        // Busca próximo movimento
        int[] nextMove = findNextMove();

        if (nextMove != null) {
            x = nextMove[0];
            y = nextMove[1];
            return true;
        } else {
            // Backtracking ou reposicionamento
//            if (!doBacktracking()) {
//                respawn();
//            }
            return true;
        }
    }

    /**
     * Encontra o próximo movimento válido
     */
    private int[] findNextMove() {
        // Direções: Norte, Sul, Leste, Oeste
        int[][] directions = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
        List<int[]> validMoves = new ArrayList<>();

        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];
            String newPos = newX + "," + newY;

            if (maze.isValidPosition(newX, newY) && !visitedPositions.contains(newPos)) {
                validMoves.add(new int[]{newX, newY});
            }
        }

        if (!validMoves.isEmpty()) {
            // Ordena por distância até o destino (heurística)
            validMoves.sort((pos1, pos2) -> {
                int dist1 = Math.abs(pos1[0] - maze.getEndX()) + Math.abs(pos1[1] - maze.getEndY());
                int dist2 = Math.abs(pos2[0] - maze.getEndX()) + Math.abs(pos2[1] - maze.getEndY());
                return Integer.compare(dist1, dist2);
            });

            // Adiciona um pouco de aleatoriedade para evitar que todos sigam o mesmo caminho
            int choiceRange = Math.min(3, validMoves.size());
            return validMoves.get(random.nextInt(choiceRange));
        }

        return null;
    }

    /**
     * Implementa backtracking
     */
    private boolean doBacktracking() {
        if (pathStack.isEmpty()) return false;

        // Remove posição atual
        pathStack.pop();

        while (!pathStack.isEmpty()) {
            int[] backPos = pathStack.peek();
            int backX = backPos[0];
            int backY = backPos[1];

            // Verifica se tem movimentos não explorados
            int[][] directions = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
            for (int[] dir : directions) {
                int checkX = backX + dir[0];
                int checkY = backY + dir[1];
                String checkPos = checkX + "," + checkY;

                if (maze.isValidPosition(checkX, checkY) && !visitedPositions.contains(checkPos)) {
                    x = backX;
                    y = backY;
                    return true;
                }
            }
            pathStack.pop();
        }
        return false;
    }

    /**
     * Reposiciona o rato quando ele fica completamente preso
     */
//    private void respawn() {
//        visitedPositions.clear();
//        pathStack.clear();
//        setRandomPosition();
//        hasReachedEnd = false;
//        System.out.println("🔄 Rato " + id + " foi reposicionado!");
//    }

    /**
     * Reinicia o rato após chegar ao destino
     */
    public void restart() {
        visitedPositions.clear();
        pathStack.clear();
        hasReachedEnd = false;
        setRandomPosition();
    }

    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getId() { return id; }
    public String getSymbol() { return symbol; }
    public boolean hasReachedEnd() { return hasReachedEnd; }
}