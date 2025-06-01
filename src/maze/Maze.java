package maze;

import models.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Maze {
    // Constantes públicas
    public static final int WALL = 0;
    public static final int PATH = 1;

    // Atributos principais
    private int[][] matrix;
    private int width;
    private int height;
    private int startX, startY;
    private int endX, endY;
    private int mouseX, mouseY; // Posição atual do mouse
    private Random random;

    // Controle de pathfinding
    private Set<String> visitedPositions = new HashSet<>();
    private Stack<int[]> pathStack = new Stack<>();
    private boolean gameRunning = true;

    /**
     * Construtor da classe Maze
     */
    public Maze(int[][] matrix, int width, int height) {
        this.matrix = matrix;
        this.width = width;
        this.height = height;
        this.random = new Random();

        // Define posição de saída fixa
        this.endX = width - 2;
        this.endY = height - 1;

        // Define posição inicial aleatória
        setRandomStartPosition();
    }

    /**
     * Define uma posição inicial aleatória válida para o mouse
     */
    private void setRandomStartPosition() {
        List<int[]> validPositions = new ArrayList<>();

        // Encontra todas as posições válidas no labirinto
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix[y][x] == PATH && !(x == endX && y == endY)) {
                    validPositions.add(new int[]{x, y});
                }
            }
        }

        if (!validPositions.isEmpty()) {
            int randomIndex = random.nextInt(validPositions.size());
            int[] chosenPosition = validPositions.get(randomIndex);

            this.startX = chosenPosition[1];
            this.startY = chosenPosition[1];
            this.mouseX = this.startX;
            this.mouseY = this.startY;

            System.out.println("🐭 Mouse apareceu em: (" + startX + ", " + startY + ")");
        } else {
            // Fallback para posição padrão
            this.startX = 1;
            this.startY = 1;
            this.mouseX = this.startX;
            this.mouseY = this.startY;
            System.out.println("⚠️ Usando posição padrão: (" + startX + ", " + startY + ")");
        }
    }

    /**
     * Método principal de renderização/movimento
     */
    public void onRender() {
        if (!gameRunning) return;

        // Verifica se chegou ao destino
        if (mouseX == endX && mouseY == endY) {
            System.out.println("🎉 PARABÉNS! Mouse chegou ao destino!");
            gameRunning = false;

            return;
        }

        // Busca próximo movimento
        int[] nextMove = getNextMove();

        if (nextMove != null) {
            mouseX = nextMove[0];
            mouseY = nextMove[1];
            System.out.println("🐭 Mouse moveu para: (" + mouseX + ", " + mouseY + ")");
        } else {
            System.out.println("🚫 Sem movimentos válidos! Reiniciando...");
        }

        // Exibe o labirinto atualizado
        display();
    }

    /**
     * Algoritmo de pathfinding melhorado
     */
    private int[] getNextMove() {
        String currentPos = mouseX + "," + mouseY;

        // Adiciona posição atual ao histórico
        if (!visitedPositions.contains(currentPos)) {
            visitedPositions.add(currentPos);
            pathStack.push(new int[]{mouseX, mouseY});
        }

        // Direções: Norte, Sul, Leste, Oeste
        int[][] directions = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
        List<int[]> validMoves = new ArrayList<>();

        // Verifica cada direção
        for (int[] dir : directions) {
            int newX = mouseX + dir[0];
            int newY = mouseY + dir[1];
            String newPos = newX + "," + newY;

            // Verifica se é posição válida e não visitada
            if (isValidPosition(newX, newY) && !visitedPositions.contains(newPos)) {
                validMoves.add(new int[]{newX, newY});
            }
        }

        // Se há movimentos válidos, escolhe o melhor (mais próximo do destino)
        if (!validMoves.isEmpty()) {
            validMoves.sort((pos1, pos2) -> {
                int dist1 = Math.abs(pos1[0] - endX) + Math.abs(pos1[1] - endY);
                int dist2 = Math.abs(pos2[0] - endX) + Math.abs(pos2[1] - endY);
                return Integer.compare(dist1, dist2);
            });
            return validMoves.get(0);
        }

        // Se não há movimentos válidos, faz backtracking
        return doBacktracking();
    }

    /**
     * Implementa backtracking quando não há movimentos válidos
     */
    private int[] doBacktracking() {
        System.out.println("🔄 Fazendo backtracking...");

        // Remove posição atual
        if (!pathStack.isEmpty()) {
            pathStack.pop();
        }

        // Procura posição anterior com movimentos válidos
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

                if (isValidPosition(checkX, checkY) && !visitedPositions.contains(checkPos)) {
                    System.out.println("🔙 Voltando para: (" + backX + ", " + backY + ")");
                    return new int[]{backX, backY};
                }
            }

            // Remove esta posição se não tem movimentos válidos
            pathStack.pop();
        }

        return null; // Não encontrou caminho
    }

    /**
     * Inicia o jogo com velocidade especificada
     */
    public void play(int velocityMs) {
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::onRender, 0, velocityMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Exibe o labirinto no console
     */
    public void display() {
        // Limpa tela
        for (int i = 0; i < 30; i++) {
            System.out.println();
        }

        System.out.println("=== LABIRINTO ===");
        System.out.println("█ = Parede | · = Caminho | * = Início | # = Saída | @ = Mouse");
        System.out.println();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Verifica se é a posição atual do mouse
                if (i == mouseY && j == mouseX) {
                    System.out.print("@ ");
                }
                // Posição de saída
                else if (i == endY && j == endX) {
                    System.out.print("# ");
                }
                // Posição inicial (apenas para referência)
                else if (i == startY && j == startX && !(i == mouseY && j == mouseX)) {
                    System.out.print("* ");
                }
                // Parede
                else if (matrix[i][j] == WALL) {
                    System.out.print("█ ");
                }
                // Caminho livre
                else {
                    System.out.print("· ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Verifica se uma posição é válida para movimento
     */
    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < width &&
                y >= 0 && y < height &&
                matrix[y][x] == PATH;
    }

    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
    public int getEndX() { return endX; }
    public int getEndY() { return endY; }
    public int[][] getMatrix() { return matrix; }

    /**
     * Exibe informações do labirinto
     */
    public void printInfo() {
        System.out.println("=== INFORMAÇÕES DO LABIRINTO ===");
        System.out.println("Dimensões: " + width + "x" + height);
        System.out.println("🐭 Posição inicial: (" + startX + ", " + startY + ")");
        System.out.println("🏁 Destino: (" + endX + ", " + endY + ")");

        int distance = Math.abs(startX - endX) + Math.abs(startY - endY);
        System.out.println("📏 Distância Manhattan: " + distance);
        System.out.println();
    }
}