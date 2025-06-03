package models;

import maze.Maze;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Mouse {
    // Atributos protegidos por sincronização
    protected volatile int x, y;
    protected volatile int id;
    protected volatile String symbol;
    protected volatile boolean hasReachedEnd = false;

    // Atributos que precisam de sincronização mais complexa
    private Maze maze;
    private Set<String> visitedPositions = Collections.synchronizedSet(new HashSet<>());
    private Stack<int[]> pathStack = new Stack<>(); // Será sincronizado manualmente
    private Random random = new Random();
    private volatile int stuckCounter = 0;
    private static final int MAX_STUCK_ATTEMPTS = 3;

    // Locks para operações críticas
    private final ReentrantLock movementLock = new ReentrantLock();
    private final ReentrantLock pathLock = new ReentrantLock();

    public Mouse(int id, Maze maze) {
        this.id = id;
        this.maze = maze;
        // Símbolos diferentes para cada rato
        String[] symbols = {"@", "♦", "♣", "♠", "♥", "◆", "◇", "★", "☆", "●"};
        this.symbol = symbols[id % symbols.length];
        setRandomPosition();
    }

    /**
     * Define uma posição inicial aleatória para este rato (thread-safe)
     */
    private void setRandomPosition() {
        movementLock.lock();
        try {
            List<int[]> validPositions = new ArrayList<>();

            // Encontra todas as posições válidas
            for (int row = 0; row < maze.getHeight(); row++) {
                for (int col = 0; col < maze.getWidth(); col++) {
                    if (maze.isValidPosition(col, row) &&
                            !maze.isEndPosition(col, row) &&
                            !maze.isPositionOccupied(col, row, this.id)) {
                        validPositions.add(new int[]{col, row});
                    }
                }
            }

            if (!validPositions.isEmpty()) {
                int randomIndex = random.nextInt(validPositions.size());
                int[] position = validPositions.get(randomIndex);
                this.x = position[0];
                this.y = position[1];
                System.out.println("🐭 Rato " + id + " (" + symbol + ") apareceu em: (" + x + ", " + y + ") [Thread: " + Thread.currentThread().getName() + "]");
            } else {
                this.x = 1;
                this.y = 0;
                System.out.println("⚠️ Rato " + id + " usando posição padrão [Thread: " + Thread.currentThread().getName() + "]");
            }
        } finally {
            movementLock.unlock();
        }
    }

    /**
     * Move o rato um passo em direção ao objetivo (thread-safe)
     */
    public boolean move() {
        if (hasReachedEnd) return false;

        movementLock.lock();
        try {
            // Verifica se chegou ao destino
            if (x == maze.getEndX() && y == maze.getEndY()) {
                hasReachedEnd = true;
                System.out.println("🎉 RATO " + id + " (" + symbol + ") CHEGOU AO DESTINO! [Thread: " +
                        Thread.currentThread().getName() + "]");
                return true;
            }

            String currentPos = x + "," + y;

            // Adiciona posição atual ao histórico
            if (!visitedPositions.contains(currentPos)) {
                visitedPositions.add(currentPos);
                pathLock.lock();
                try {
                    pathStack.push(new int[]{x, y});
                } finally {
                    pathLock.unlock();
                }
                stuckCounter = 0; // Reset contador quando encontra novo caminho
            }

            // Busca próximo movimento
            int[] nextMove = findNextMove();

            if (nextMove != null) {
                x = nextMove[0];
                y = nextMove[1];
                stuckCounter = 0; // Reset contador quando se move
                return true;
            } else {
                stuckCounter++;

                // Se ficou preso muitas vezes, tenta estratégias mais agressivas
                if (stuckCounter >= MAX_STUCK_ATTEMPTS) {
                    return handleStuckSituation();
                } else {
                    // Tenta backtracking normal primeiro
                    return doBacktracking();
                }
            }
        } finally {
            movementLock.unlock();
        }
    }

    /**
     * Lida com situações onde o rato está completamente preso (thread-safe)
     */
    private boolean handleStuckSituation() {
        System.out.println("🔄 Rato " + id + " está explorando novo caminho... [Thread: " +
                Thread.currentThread().getName() + "]");

        // Estratégia 1: Limpar uma parte do histórico e tentar backtrack mais profundo
        if (clearPartialHistory()) {
            stuckCounter = 0;
            return true;
        }

        // Estratégia 2: Buscar o caminho não explorado mais próximo
        if (moveToNearestUnexplored()) {
            stuckCounter = 0;
            return true;
        }

        // Estratégia 3: Reset parcial - limpa histórico mas mantém posição
        resetExplorationState();
        stuckCounter = 0;
        return true;
    }

    /**
     * Limpa parte do histórico de posições visitadas (thread-safe)
     */
    private boolean clearPartialHistory() {
        if (visitedPositions.size() <= 5) return false;

        // Remove 30% das posições visitadas mais antigas
        List<String> positionsList = new ArrayList<>(visitedPositions);
        int toRemove = Math.max(1, positionsList.size() / 3);

        for (int i = 0; i < toRemove; i++) {
            visitedPositions.remove(positionsList.get(i));
        }

        // Tenta encontrar um movimento agora
        int[] nextMove = findNextMove();
        if (nextMove != null) {
            x = nextMove[0];
            y = nextMove[1];
            return true;
        }

        return false;
    }

    /**
     * Move o rato para a área não explorada mais próxima (thread-safe)
     */
    private boolean moveToNearestUnexplored() {
        List<int[]> unexploredPositions = new ArrayList<>();

        // Encontra todas as posições válidas não visitadas
        for (int row = 0; row < maze.getHeight(); row++) {
            for (int col = 0; col < maze.getWidth(); col++) {
                String pos = col + "," + row;
                if (maze.isValidPosition(col, row) &&
                        !visitedPositions.contains(pos) &&
                        !maze.isEndPosition(col, row)) {
                    unexploredPositions.add(new int[]{col, row});
                }
            }
        }

        if (unexploredPositions.isEmpty()) return false;

        // Encontra a posição não explorada mais próxima
        int[] nearestPos = unexploredPositions.get(0);
        int minDistance = manhattanDistance(x, y, nearestPos[0], nearestPos[1]);

        for (int[] pos : unexploredPositions) {
            int distance = manhattanDistance(x, y, pos[0], pos[1]);
            if (distance < minDistance) {
                minDistance = distance;
                nearestPos = pos;
            }
        }

        // Move em direção à posição não explorada mais próxima
        return moveTowardsTarget(nearestPos[0], nearestPos[1]);
    }

    /**
     * Move um passo em direção a uma posição alvo (thread-safe)
     */
    private boolean moveTowardsTarget(int targetX, int targetY) {
        int[][] directions = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
        int[] bestMove = null;
        int bestDistance = Integer.MAX_VALUE;

        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];

            if (maze.isValidPosition(newX, newY) &&
                    !maze.isPositionOccupied(newX, newY, this.id)) {
                int distance = manhattanDistance(newX, newY, targetX, targetY);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMove = new int[]{newX, newY};
                }
            }
        }

        if (bestMove != null) {
            x = bestMove[0];
            y = bestMove[1];
            return true;
        }

        return false;
    }

    /**
     * Calcula distância Manhattan entre dois pontos
     */
    private int manhattanDistance(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    /**
     * Reset do estado de exploração mantendo a posição atual (thread-safe)
     */
    private void resetExplorationState() {
        pathLock.lock();
        try {
            // Mantém apenas as últimas 5 posições visitadas para evitar loops imediatos
            if (visitedPositions.size() > 5) {
                Set<String> recentPositions = Collections.synchronizedSet(new HashSet<>());
                Stack<int[]> tempStack = new Stack<>();

                // Preserva as 5 posições mais recentes
                for (int i = 0; i < Math.min(5, pathStack.size()); i++) {
                    int[] pos = pathStack.pop();
                    tempStack.push(pos);
                    recentPositions.add(pos[0] + "," + pos[1]);
                }

                // Restaura as posições recentes
                pathStack.clear();
                visitedPositions.clear();
                while (!tempStack.isEmpty()) {
                    pathStack.push(tempStack.pop());
                }
                visitedPositions.addAll(recentPositions);
            }
        } finally {
            pathLock.unlock();
        }

        System.out.println("🔄 Rato " + id + " resetou exploração e continua explorando [Thread: " +
                Thread.currentThread().getName() + "]");
    }

    /**
     * Encontra o próximo movimento válido (thread-safe)
     */
    private int[] findNextMove() {
        // Direções: Norte, Sul, Leste, Oeste
        int[][] directions = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
        List<int[]> validMoves = new ArrayList<>();

        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];
            String newPos = newX + "," + newY;

            if (maze.isValidPosition(newX, newY) &&
                    !visitedPositions.contains(newPos) &&
                    !maze.isPositionOccupied(newX, newY, this.id)) {
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
     * Implementa backtracking melhorado (thread-safe)
     */
    private boolean doBacktracking() {
        pathLock.lock();
        try {
            if (pathStack.isEmpty()) return false;

            // Remove posição atual do stack
            if (!pathStack.isEmpty()) {
                pathStack.pop();
            }

            // Procura por uma posição anterior que ainda tenha movimentos válidos
            while (!pathStack.isEmpty()) {
                int[] backPos = pathStack.peek();
                int backX = backPos[0];
                int backY = backPos[1];

                // Verifica se tem movimentos não explorados desta posição
                int[][] directions = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
                for (int[] dir : directions) {
                    int checkX = backX + dir[0];
                    int checkY = backY + dir[1];
                    String checkPos = checkX + "," + checkY;

                    if (maze.isValidPosition(checkX, checkY) &&
                            !visitedPositions.contains(checkPos) &&
                            !maze.isPositionOccupied(checkX, checkY, this.id)) {
                        x = backX;
                        y = backY;
                        return true;
                    }
                }
                pathStack.pop();
            }

            return false;
        } finally {
            pathLock.unlock();
        }
    }

    /**
     * Reinicia o rato após chegar ao destino (thread-safe)
     */
    public void restart() {
        movementLock.lock();
        pathLock.lock();
        try {
            visitedPositions.clear();
            pathStack.clear();
            hasReachedEnd = false;
            stuckCounter = 0;
            setRandomPosition();
        } finally {
            pathLock.unlock();
            movementLock.unlock();
        }
    }

    // Getters thread-safe
    public int getX() { return x; }
    public int getY() { return y; }
    public int getId() { return id; }
    public String getSymbol() { return symbol; }
    public boolean hasReachedEnd() { return hasReachedEnd; }
}