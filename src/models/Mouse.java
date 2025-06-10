package models;

import maze.Maze;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Mouse implements Runnable {
    // Atributos protegidos por sincronização
    protected volatile int x, y;
    protected volatile int id;
    protected volatile String symbol;
    protected volatile boolean hasReachedEnd = false;
    protected volatile boolean isRunning = true;

    // Atributos que precisam de sincronização mais complexa
    private Maze maze;
    private Set<String> visitedPositions = Collections.synchronizedSet(new HashSet<>());
    private Stack<int[]> pathStack = new Stack<>();
    private Random random = new Random();
    private volatile int stuckCounter = 0;
    private static final int MAX_STUCK_ATTEMPTS = 5;
    private int velocityMs;

    // Locks para operações críticas
    private final ReentrantLock movementLock = new ReentrantLock();
    private final ReentrantLock pathLock = new ReentrantLock();

    public Mouse(int id, Maze maze, int velocityMs) {
        this.id = id;
        this.maze = maze;
        this.velocityMs = velocityMs;
        // Símbolos diferentes para cada rato
        String[] symbols = {"@", "♦", "♣", "♠", "♥", "◆", "◇", "★", "☆", "●"};
        this.symbol = symbols[id % symbols.length];
        setInitialPosition();
    }

    /**
     * Define uma posição inicial válida para este rato (thread-safe)
     */
    private void setInitialPosition() {
        movementLock.lock();
        try {
            List<int[]> validPositions = new ArrayList<>();

            // Encontra todas as posições válidas
            for (int row = 1; row < maze.getHeight() - 1; row++) {
                for (int col = 1; col < maze.getWidth() - 1; col++) {
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
                System.out.println("🐭 Rato " + id + " (" + symbol + ") iniciou em: (" + x + ", " + y + ")");
            } else {
                // Posição padrão se não encontrar espaço
                this.x = 1;
                this.y = 1;
                System.out.println("⚠️ Rato " + id + " usando posição padrão (1,1)");
            }
        } finally {
            movementLock.unlock();
        }
    }

    /**
     * Método principal da thread - executa o movimento contínuo do rato
     */
    @Override
    public void run() {
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                // Se chegou ao destino, para de se mover mas não reinicia
                if (hasReachedEnd) {
                    System.out.println("🎯 Rato " + id + " permanece no destino!");
                    Thread.sleep(5000); // Espera 5 segundos antes de verificar novamente
                    continue;
                }

                // Tenta mover o rato
                boolean moved = move();

                if (!moved) {
                    // Se não conseguiu se mover, tenta estratégias de recuperação
                    handleStuckSituation();
                }

                // Pausa entre movimentos com pequena variação
                int variation = random.nextInt(velocityMs / 4);
                Thread.sleep(velocityMs + variation - (velocityMs / 8));

            } catch (InterruptedException e) {
                System.out.println("🛑 Thread do rato " + id + " foi interrompida");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("❌ Erro na thread do rato " + id + ": " + e.getMessage());
            }
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
                System.out.println("🎉 RATO " + id + " (" + symbol + ") CHEGOU AO DESTINO!");
                return true;
            }

            String currentPos = x + "," + y;

            // Adiciona posição atual ao histórico se é nova
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
                stuckCounter = 0;
                return true;
            } else {
                stuckCounter++;
                // Se não encontrou movimento, tenta backtracking
                return doBacktracking();
            }
        } finally {
            movementLock.unlock();
        }
    }

    /**
     * Lida com situações onde o rato está preso (thread-safe)
     */
    private boolean handleStuckSituation() {
        if (stuckCounter >= MAX_STUCK_ATTEMPTS) {
            System.out.println("🔄 Rato " + id + " explorando nova rota...");

            // Estratégia 1: Limpar parte do histórico
            if (clearPartialHistory()) {
                return true;
            }

            // Estratégia 2: Mover para área não explorada
            if (moveToNearestUnexplored()) {
                return true;
            }

            // Estratégia 3: Reset parcial do histórico
            resetPartialExploration();
            stuckCounter = 0;
        }
        return false;
    }

    /**
     * Limpa parte do histórico para permitir re-exploração (thread-safe)
     */
    private boolean clearPartialHistory() {
        if (visitedPositions.size() <= 10) return false;

        // Remove 40% das posições mais antigas
        List<String> positionsList = new ArrayList<>(visitedPositions);
        int toRemove = Math.max(5, positionsList.size() * 2 / 5);

        for (int i = 0; i < toRemove && i < positionsList.size(); i++) {
            visitedPositions.remove(positionsList.get(i));
        }

        // Tenta encontrar movimento após limpeza
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

        // Encontra posições válidas não visitadas
        for (int row = 1; row < maze.getHeight() - 1; row++) {
            for (int col = 1; col < maze.getWidth() - 1; col++) {
                String pos = col + "," + row;
                if (maze.isValidPosition(col, row) && !visitedPositions.contains(pos)) {
                    unexploredPositions.add(new int[]{col, row});
                }
            }
        }

        if (unexploredPositions.isEmpty()) return false;

        // Encontra a posição mais próxima
        int[] nearestPos = unexploredPositions.get(0);
        int minDistance = manhattanDistance(x, y, nearestPos[0], nearestPos[1]);

        for (int[] pos : unexploredPositions) {
            int distance = manhattanDistance(x, y, pos[0], pos[1]);
            if (distance < minDistance) {
                minDistance = distance;
                nearestPos = pos;
            }
        }

        // Move em direção à posição mais próxima
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
     * Reset parcial do estado de exploração (thread-safe)
     */
    private void resetPartialExploration() {
        pathLock.lock();
        try {
            // Mantém apenas as últimas 8 posições para evitar loops
            if (visitedPositions.size() > 8) {
                Set<String> recentPositions = Collections.synchronizedSet(new HashSet<>());
                Stack<int[]> tempStack = new Stack<>();

                // Preserva as 8 posições mais recentes
                int preserve = Math.min(8, pathStack.size());
                for (int i = 0; i < preserve; i++) {
                    int[] pos = pathStack.pop();
                    tempStack.push(pos);
                    recentPositions.add(pos[0] + "," + pos[1]);
                }

                // Restaura apenas as posições recentes
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

        System.out.println("🔄 Rato " + id + " resetou exploração parcial");
    }

    /**
     * Encontra o próximo movimento válido usando heurística (thread-safe)
     */
    private int[] findNextMove() {
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
            // Ordena por distância até o destino (heurística A*)
            validMoves.sort((pos1, pos2) -> {
                int dist1 = manhattanDistance(pos1[0], pos1[1], maze.getEndX(), maze.getEndY());
                int dist2 = manhattanDistance(pos2[0], pos2[1], maze.getEndX(), maze.getEndY());
                return Integer.compare(dist1, dist2);
            });

            // Adiciona aleatoriedade aos 2 melhores movimentos
            int choiceRange = Math.min(2, validMoves.size());
            return validMoves.get(random.nextInt(choiceRange));
        }

        return null;
    }

    /**
     * Implementa backtracking inteligente (thread-safe)
     */
    private boolean doBacktracking() {
        pathLock.lock();
        try {
            if (pathStack.isEmpty()) return false;

            // Remove posição atual do stack
            if (!pathStack.isEmpty()) {
                pathStack.pop();
            }

            // Procura posição anterior com movimentos válidos
            while (!pathStack.isEmpty()) {
                int[] backPos = pathStack.peek();
                int backX = backPos[0];
                int backY = backPos[1];

                // Verifica se há movimentos não explorados
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
     * Para a execução desta thread
     */
    public void stop() {
        isRunning = false;
    }

    // Getters thread-safe
    public int getX() { return x; }
    public int getY() { return y; }
    public int getId() { return id; }
    public String getSymbol() { return symbol; }
    public boolean hasReachedEnd() { return hasReachedEnd; }
}