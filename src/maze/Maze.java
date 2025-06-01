package maze;

import models.Mouse;
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
    private int endX, endY;
    private Random random;

    // Controle de múltiplos ratos
    private List<Mouse> mice = new ArrayList<>();
    private boolean gameRunning = true;
    private ScheduledExecutorService executor;

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
    }

    /**
     * Adiciona um rato ao labirinto
     */
    public void addMouse() {
        Mouse mouse = new Mouse(mice.size(), this);
        mice.add(mouse);
        System.out.println("➕ Adicionado rato " + mouse.getId() + " (" + mouse.getSymbol() + ")");
    }

    /**
     * Adiciona múltiplos ratos ao labirinto
     */
    public void addMice(int count) {
        for (int i = 0; i < count; i++) {
            addMouse();
        }
        System.out.println("🐭 Total de " + mice.size() + " ratos no labirinto!");
    }

    /**
     * Método principal de renderização/movimento
     */
    public void onRender() {
        if (!gameRunning || mice.isEmpty()) return;

        // Move todos os ratos em paralelo
        for (Mouse mouse : mice) {
            if (!mouse.hasReachedEnd()) {
                mouse.move();
            }
        }

        // Verifica se algum rato chegou ao destino
        for (Mouse mouse : mice) {
            if (mouse.hasReachedEnd()) {
                // Aguarda 3 segundos e reinicia o rato
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mouse.restart();
                        System.out.println("🔄 Rato " + mouse.getId() + " reiniciado!");
                    }
                }, 3000);
            }
        }

        // Exibe o labirinto atualizado
        display();
    }

    /**
     * Inicia o jogo com velocidade especificada
     */
    public void play(int velocityMs) {
        if (mice.isEmpty()) {
            System.out.println("⚠️ Adicione pelo menos um rato antes de iniciar!");
            return;
        }

        System.out.println("🎮 Iniciando simulação com " + mice.size() + " ratos...");
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::onRender, 0, velocityMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Para a simulação
     */
    public void stop() {
        gameRunning = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Exibe o labirinto no console
     */
    public void display() {
        // Limpa tela
        for (int i = 0; i < 30; i++) {
            System.out.println();
        }

        System.out.println("=== LABIRINTO COM MÚLTIPLOS RATOS ===");
        System.out.println("█ = Parede | · = Caminho | # = Saída");

        // Mostra informações dos ratos
        System.out.print("Ratos ativos: ");
        for (Mouse mouse : mice) {
            if (!mouse.hasReachedEnd()) {
                System.out.print(mouse.getSymbol() + "(ID:" + mouse.getId() + ") ");
            }
        }
        System.out.println();
        System.out.println();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                boolean mouseHere = false;
                String mouseSymbol = "";

                // Verifica se há algum rato nesta posição
                for (Mouse mouse : mice) {
                    if (mouse.getX() == j && mouse.getY() == i && !mouse.hasReachedEnd()) {
                        mouseHere = true;
                        mouseSymbol = mouse.getSymbol();
                        break; // Se há múltiplos ratos na mesma posição, mostra apenas o primeiro
                    }
                }

                if (mouseHere) {
                    System.out.print(mouseSymbol + " ");
                }
                // Posição de saída
                else if (i == endY && j == endX) {
                    System.out.print("# ");
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

    /**
     * Verifica se uma posição é o destino
     */
    public boolean isEndPosition(int x, int y) {
        return x == endX && y == endY;
    }

    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getEndX() { return endX; }
    public int getEndY() { return endY; }
    public int[][] getMatrix() { return matrix; }
    public List<Mouse> getMice() { return mice; }

    /**
     * Exibe informações do labirinto
     */
    public void printInfo() {
        System.out.println("=== INFORMAÇÕES DO LABIRINTO ===");
        System.out.println("Dimensões: " + width + "x" + height);
        System.out.println("🏁 Destino: (" + endX + ", " + endY + ")");
        System.out.println("🐭 Ratos no labirinto: " + mice.size());
        System.out.println();
    }
}