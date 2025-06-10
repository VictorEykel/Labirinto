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
    private ScheduledExecutorService displayExecutor;
    private List<Thread> mouseThreads = Collections.synchronizedList(new ArrayList<>());

    // Sincronização para posições dos ratos
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

        // Adiciona hook para limpeza ao encerrar
        addShutdownHook();
    }

    /**
     * Adiciona um rato ao labirinto
     */
    public void addMouse(int velocityMs) {
        Mouse mouse = new Mouse(mice.size(), this, velocityMs);
        synchronized(mice) {
            mice.add(mouse);
        }
        System.out.println("➕ Adicionado rato " + mouse.getId() + " (" + mouse.getSymbol() + ")");
    }

    /**
     * Adiciona múltiplos ratos ao labirinto
     */
    public void addMice(int count, int velocityMs) {
        for (int i = 0; i < count; i++) {
            addMouse(velocityMs);
        }
        System.out.println("🐭 Total de " + mice.size() + " ratos no labirinto!");
    }

    /**
     * Inicia o jogo com velocidade especificada usando threads nativas
     */
    public void play(int velocityMs) {
        if (mice.isEmpty()) {
            System.out.println("Adicione pelo menos um rato antes de iniciar!");
            return;
        }

        // Inicia uma thread dedicada para cada rato
        synchronized(mice) {
            for (Mouse mouse : mice) {
                Thread mouseThread = new Thread(mouse, "RatoThread-" + mouse.getId());
                mouseThread.setDaemon(false); // Thread não-daemon para manter programa vivo
                mouseThreads.add(mouseThread);
                mouseThread.start();
                System.out.println("Thread iniciada para rato " + mouse.getId());
            }
        }

        // Thread para atualizar a exibição periodicamente
        displayExecutor = Executors.newSingleThreadScheduledExecutor();
        displayExecutor.scheduleAtFixedRate(this::updateDisplay, 1000, velocityMs, TimeUnit.MILLISECONDS);

    }

    /**
     * Atualiza a exibição do labirinto (thread-safe)
     */
    private void updateDisplay() {
        if (!gameRunning) return;

        synchronized(displayLock) {
            display();

            // Verifica se todos os ratos chegaram ao destino
            boolean allFinished = true;
            synchronized(mice) {
                for (Mouse mouse : mice) {
                    if (!mouse.hasReachedEnd()) {
                        allFinished = false;
                        break;
                    }
                }
            }

            if (allFinished && !mice.isEmpty()) {
                System.out.println("\n🎊 PARABÉNS! TODOS OS RATOS CHEGARAM AO DESTINO! 🎊");
                stop();
            }
        }
    }

    /**
     * Para a simulação e todas as threads de forma segura
     */
    public void stop() {
        gameRunning = false;

        // Para todos os ratos
        synchronized(mice) {
            for (Mouse mouse : mice) {
                mouse.stop();
            }
        }

        // Interrompe todas as threads dos ratos
        synchronized(mouseThreads) {
            for (Thread thread : mouseThreads) {
                if (thread.isAlive()) {
                    thread.interrupt();
                }
            }
        }

        // Para a thread de exibição
        if (displayExecutor != null && !displayExecutor.isShutdown()) {
            displayExecutor.shutdownNow();
        }

        // Aguarda finalização das threads
        synchronized(mouseThreads) {
            for (Thread thread : mouseThreads) {
                try {
                    thread.join(2000); // Aguarda até 2 segundos por thread
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        System.out.println("✅ Todas as threads foram finalizadas");
    }

    /**
     * Exibe o labirinto no console (thread-safe)
     */
    public void display() {
        // Cria snapshot das posições dos ratos para renderização segura
        List<MouseSnapshot> activeMouses = new ArrayList<>();
        int finishedCount = 0;

        synchronized(mice) {
            for (Mouse mouse : mice) {
                if (mouse.hasReachedEnd()) {
                    finishedCount++;
                }
                // Mostra todos os ratos, mesmo os que chegaram ao destino
                activeMouses.add(new MouseSnapshot(mouse.getX(), mouse.getY(),
                        mouse.getId(), mouse.getSymbol(),
                        mouse.hasReachedEnd()));
            }
        }

        // Limpa tela (funciona na maioria dos terminais)

        System.out.println("=== LABIRINTO COM MÚLTIPLOS RATOS ===");
        System.out.println("█ = Parede | · = Caminho | # = Saída");
        System.out.println("Ratos explorando: " + (mice.size() - finishedCount));
        System.out.println("Chegaram ao destino: " + finishedCount);

        // Mostra informações dos ratos
        System.out.print("Status dos ratos: ");
        for (MouseSnapshot mouse : activeMouses) {
            if (mouse.hasReachedEnd) {
                System.out.print(mouse.symbol + "(✓) ");
            } else {
                System.out.print(mouse.symbol + "(º) ");
            }
        }
        System.out.println();

        // Renderiza o labirinto
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                boolean mouseHere = false;
                String mouseSymbol = "";

                // Verifica se há algum rato nesta posição
                for (MouseSnapshot mouse : activeMouses) {
                    if (mouse.x == j && mouse.y == i) {
                        mouseHere = true;
                        // Se o rato chegou ao destino, mostra com destaque
                        mouseSymbol = mouse.hasReachedEnd ? "🎯" : mouse.symbol;
                        break;
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
    private static class MouseSnapshot {
        public final int x, y, id;
        public final String symbol;
        public final boolean hasReachedEnd;

        public MouseSnapshot(int x, int y, int id, String symbol, boolean hasReachedEnd) {
            this.x = x;
            this.y = y;
            this.id = id;
            this.symbol = symbol;
            this.hasReachedEnd = hasReachedEnd;
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
     * Permite múltiplos ratos na mesma posição para evitar bloqueios
     */
    public boolean isPositionOccupied(int x, int y, int excludeMouseId) {
        // Permitir que ratos ocupem a mesma posição temporariamente
        // Isso evita que fiquem completamente bloqueados
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
     * Adiciona shutdown hook para limpeza adequada
     */
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Encerrando sistema graciosamente...");
            stop();
        }));
    }
}