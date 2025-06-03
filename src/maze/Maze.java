package maze;

import models.Mouse;
import java.util.*;
import java.util.concurrent.*;

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

    // Controle de múltiplos ratos com threads
    private List<Mouse> mice = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean gameRunning = true;
    private ExecutorService mouseExecutor;
    private ScheduledExecutorService displayExecutor;

    // Sincronização para posições dos ratos
    private final Object positionLock = new Object();
    private final Object displayLock = new Object();

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
        synchronized(mice) {
            mice.add(mouse);
        }
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
     * Inicia o jogo com velocidade especificada usando threads
     */
    public void play(int velocityMs) {
        if (mice.isEmpty()) {
            System.out.println("⚠️ Adicione pelo menos um rato antes de iniciar!");
            return;
        }

        System.out.println("🎮 Iniciando simulação com " + mice.size() + " ratos em threads paralelas...");

        // Executor para as threads dos ratos
        mouseExecutor = Executors.newFixedThreadPool(mice.size());

        // Executor para atualização da tela
        displayExecutor = Executors.newSingleThreadScheduledExecutor();

        // Inicia uma thread para cada rato
        for (Mouse mouse : mice) {
            mouseExecutor.submit(new MouseRunner(mouse, velocityMs));
        }

        // Thread para atualizar a exibição periodicamente
        displayExecutor.scheduleAtFixedRate(this::updateDisplay, 0, velocityMs, TimeUnit.MILLISECONDS);

        System.out.println("🚀 Todas as threads dos ratos foram iniciadas!");
    }

    /**
     * Classe interna para executar cada rato em sua própria thread
     */
    private class MouseRunner implements Runnable {
        private final Mouse mouse;
        private final int velocityMs;
        private final Random threadRandom = new Random();

        public MouseRunner(Mouse mouse, int velocityMs) {
            this.mouse = mouse;
            this.velocityMs = velocityMs;
        }

        @Override
        public void run() {
            System.out.println("🧵 Thread do rato " + mouse.getId() + " iniciada!");

            while (gameRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    // Verifica se o rato chegou ao fim
                    if (mouse.hasReachedEnd()) {
                        // Aguarda um tempo antes de reiniciar
                        Thread.sleep(3000);
                        synchronized(positionLock) {
                            mouse.restart();
                        }
                        System.out.println("🔄 Rato " + mouse.getId() + " reiniciado na thread!");
                        continue;
                    }

                    // Move o rato
                    synchronized(positionLock) {
                        mouse.move();
                    }

                    // Adiciona pequena variação no tempo para tornar mais realista
                    int variation = threadRandom.nextInt(velocityMs / 4); // ±25% de variação
                    Thread.sleep(velocityMs + variation - (velocityMs / 8));

                } catch (InterruptedException e) {
                    System.out.println("🛑 Thread do rato " + mouse.getId() + " foi interrompida");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("❌ Erro na thread do rato " + mouse.getId() + ": " + e.getMessage());
                }
            }

            System.out.println("🏁 Thread do rato " + mouse.getId() + " finalizou");
        }
    }

    /**
     * Atualiza a exibição do labirinto (thread-safe)
     */
    private void updateDisplay() {
        if (!gameRunning) return;

        synchronized(displayLock) {
            display();
        }
    }

    /**
     * Para a simulação e todas as threads
     */
    public void stop() {
        System.out.println("🛑 Parando simulação...");
        gameRunning = false;

        // Para as threads dos ratos
        if (mouseExecutor != null && !mouseExecutor.isShutdown()) {
            mouseExecutor.shutdownNow();
            try {
                if (!mouseExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    System.out.println("⚠️ Algumas threads dos ratos não finalizaram no tempo esperado");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Para a thread de exibição
        if (displayExecutor != null && !displayExecutor.isShutdown()) {
            displayExecutor.shutdownNow();
        }

        System.out.println("✅ Todas as threads foram finalizadas");
    }

    /**
     * Exibe o labirinto no console (thread-safe)
     */
    public void display() {
        // Cria snapshot das posições dos ratos para evitar mudanças durante a renderização
        List<Mouse> mouseSnapshot = new ArrayList<>();
        synchronized(mice) {
            for (Mouse mouse : mice) {
                if (!mouse.hasReachedEnd()) {
                    mouseSnapshot.add(new MouseSnapshot(mouse));
                }
            }
        }

        // Limpa tela
        for (int i = 0; i < 30; i++) {
            System.out.println();
        }

        System.out.println("=== LABIRINTO COM MÚLTIPLOS RATOS (THREADS) ===");
        System.out.println("█ = Parede | · = Caminho | # = Saída");

        // Mostra informações dos ratos ativos
        System.out.print("Ratos ativos: ");
        for (Mouse mouse : mouseSnapshot) {
            System.out.print(mouse.getSymbol() + "(ID:" + mouse.getId() + ") ");
        }
        System.out.println(" | Total threads: " + (mouseSnapshot.size() + 1));
        System.out.println();

        // Renderiza o labirinto
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                boolean mouseHere = false;
                String mouseSymbol = "";

                // Verifica se há algum rato nesta posição
                for (Mouse mouse : mouseSnapshot) {
                    if (mouse.getX() == j && mouse.getY() == i) {
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
     * Classe auxiliar para snapshot thread-safe dos ratos
     */
    private static class MouseSnapshot extends Mouse {
        public MouseSnapshot(Mouse original) {
            super(original.getId(), null); // maze não é necessário para snapshot
            // Copia os dados necessários de forma thread-safe
            this.setSnapshot(original.getX(), original.getY(), original.getId(),
                    original.getSymbol(), original.hasReachedEnd());
        }

        private void setSnapshot(int x, int y, int id, String symbol, boolean reachedEnd) {
            // Métodos protegidos para configurar o snapshot
            this.x = x;
            this.y = y;
            this.id = id;
            this.symbol = symbol;
            this.hasReachedEnd = reachedEnd;
        }
    }

    /**
     * Verifica se uma posição é válida para movimento (thread-safe)
     */
    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < width &&
                y >= 0 && y < height &&
                matrix[y][x] == PATH;
    }

    /**
     * Verifica se uma posição é o destino (thread-safe)
     */
    public boolean isEndPosition(int x, int y) {
        return x == endX && y == endY;
    }

    /**
     * Verifica se há conflito de posição entre ratos (thread-safe)
     */
    public boolean isPositionOccupied(int x, int y, int excludeMouseId) {
        synchronized(mice) {
            for (Mouse mouse : mice) {
                if (mouse.getId() != excludeMouseId &&
                        mouse.getX() == x && mouse.getY() == y &&
                        !mouse.hasReachedEnd()) {
                    return true;
                }
            }
        }
        return false;
    }

    // Getters thread-safe
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getEndX() { return endX; }
    public int getEndY() { return endY; }
    public int[][] getMatrix() { return matrix; }

    public List<Mouse> getMice() {
        synchronized(mice) {
            return new ArrayList<>(mice);
        }
    }

    /**
     * Exibe informações do labirinto
     */
    public void printInfo() {
        System.out.println("=== INFORMAÇÕES DO LABIRINTO (MULTI-THREAD) ===");
        System.out.println("Dimensões: " + width + "x" + height);
        System.out.println("🏁 Destino: (" + endX + ", " + endY + ")");
        synchronized(mice) {
            System.out.println("🐭 Ratos no labirinto: " + mice.size());
        }
        System.out.println("🧵 Cada rato roda em sua própria thread");
        System.out.println("⚡ Processamento paralelo ativo");
        System.out.println();
    }

    /**
     * Adiciona shutdown hook para limpeza adequada
     */
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Finalizando sistema...");
            stop();
        }));
    }
}