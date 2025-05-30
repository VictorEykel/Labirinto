package maze;

import models.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Maze {
    // Constantes públicas que outras classes podem usar
    public static final int WALL = 0; // Valor que representa parede na matriz
    public static final int PATH = 1; // Valor que representa caminho na matriz

    // Atributos privados da classe
    private int[][] matrix;     // Matriz bidimensional do labirinto
    private int width;          // Largura do labirinto (número de colunas)
    private int height;         // Altura do labirinto (número de linhas)
    private int startX, startY; // Coordenadas do ponto de início
    private int endX, endY;     // Coordenadas do ponto de saída
    private int direction_x = 1, direction_y = 0; // Posição atual do mouse
    private Entity[] entities = null;
    private Set<String> visitedPositions = new HashSet<>(); // Mudança para Set com coordenadas
    private Stack<String> pathStack = new Stack<>(); // Pilha para backtracking
    final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();


    /**
     * Construtor da classe Maze
     */
    public Maze(int[][] matrix, int width, int height) {
        this.matrix = matrix;
        this.width = width;
        this.height = height;
        this.startX = 1;
        this.startY = 0;
        this.endX = width - 2;
        this.endY = height - 1;
        this.entities = new Entity[(width * height)+1];
    }

    public Entity getEntity(int x, int y) {
        for (Entity e : entities) {
            if (e != null && e.position_x == x && e.position_y == y) return e;
        }
        return null;
    }

    int render_index = 0;
    public void onrender(){
        if (render_index >= this.entities.length) return;
        Mouse mouse = (Mouse)this.entities[1];
        if (render_index == 0){
            render_index++;
            direction_x = startX;
            direction_y = startY;
            return;
        }

        Entity nextEntity = this.getNextEntity();
        render_index++;

        if (nextEntity == null) {
            System.out.println("Sem movimentos válidos disponíveis!");
            return;
        }

        if (nextEntity instanceof End) {
            mouse.position_x = nextEntity.position_x;
            mouse.position_y = nextEntity.position_y;
            this.direction_x = nextEntity.position_x;
            this.direction_y = nextEntity.position_y;
            this.display(false);
            System.out.println("🎉 PARABÉNS! Mouse chegou ao destino!");

            executorService.close();
            executorService.shutdown();
            return;
        }

        if (nextEntity instanceof Land) {
            mouse.position_x = nextEntity.position_x;
            mouse.position_y = nextEntity.position_y;
            this.direction_x = nextEntity.position_x;
            this.direction_y = nextEntity.position_y;
        }

        this.display(false);
    }

    public End getEnd(){
        return (End)this.entities[this.entities.length-2];
    }

    /**
     * Algoritmo de pathfinding melhorado
     * Combina busca direcionada ao objetivo com backtracking
     */
    public Entity getNextEntity(){
        String currentPos = direction_x + "," + direction_y;

        // Verifica se chegou ao destino
        if (direction_x == endX && direction_y == endY) {
            return getEnd();
        }

        // Adiciona posição atual ao histórico se ainda não estiver
        visitedPositions.add(currentPos);
        pathStack.push(currentPos);

        // Array de direções: Norte, Sul, Leste, Oeste
        int[][] directions = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};

        // Lista para armazenar movimentos válidos
        List<Entity> validMoves = new ArrayList<>();

        // Verifica cada direção
        for (int[] dir : directions) {
            int newX = direction_x + dir[0];
            int newY = direction_y + dir[1];
            String newPos = newX + "," + newY;

            // Verifica se é uma posição válida
            if (isValidPosition(newX, newY)) {
                Entity entity = getEntity(newX, newY);

                // Se chegou ao destino, retorna imediatamente
                if (newX == endX && newY == endY) {
                    System.out.println("Destino encontrado!");
                    return getEnd();
                }

                // Se é uma posição não visitada
                if (!visitedPositions.contains(newPos) && entity instanceof Land) {
                    validMoves.add(entity);
                }
            }
        }

        // Se há movimentos válidos, escolhe o melhor
        if (!validMoves.isEmpty()) {
            // Ordena por distância Manhattan até o destino
            validMoves.sort((e1, e2) -> {
                int dist1 = Math.abs(e1.position_x - endX) + Math.abs(e1.position_y - endY);
                int dist2 = Math.abs(e2.position_x - endX) + Math.abs(e2.position_y - endY);
                return Integer.compare(dist1, dist2);
            });

            Entity chosen = validMoves.get(0);
            System.out.println("Movendo para: (" + chosen.position_x + "," + chosen.position_y + ")");
            return chosen;
        }

        // Se não há movimentos válidos, faz backtracking
        System.out.println("Fazendo backtracking...");
        return doBacktracking();
    }

    /**
     * Implementa backtracking quando não há movimentos válidos
     */
    private Entity doBacktracking() {
        if (pathStack.isEmpty()) {
            System.out.println("Erro: Pilha de backtracking vazia!");
            return null;
        }

        // Remove a posição atual da pilha
        pathStack.pop();

        // Procura por uma posição anterior que tenha movimentos válidos
        while (!pathStack.isEmpty()) {
            String backPos = pathStack.peek();
            String[] coords = backPos.split(",");
            int backX = Integer.parseInt(coords[0]);
            int backY = Integer.parseInt(coords[1]);

            // Verifica se essa posição tem movimentos não explorados
            int[][] directions = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
            for (int[] dir : directions) {
                int checkX = backX + dir[0];
                int checkY = backY + dir[1];
                String checkPos = checkX + "," + checkY;

                if (isValidPosition(checkX, checkY) && !visitedPositions.contains(checkPos)) {
                    // Encontrou uma posição válida, volta para lá
                    Entity backEntity = getEntity(backX, backY);
                    if (backEntity instanceof Land) {
                        System.out.println("Voltando para: (" + backX + "," + backY + ")");
                        return backEntity;
                    }
                }
            }

            // Se essa posição não tem movimentos válidos, remove e continua
            pathStack.pop();
        }

        System.out.println("Não foi possível encontrar um caminho!");
        return null;
    }

    public void play(int velocity) throws InterruptedException {
        executorService.scheduleAtFixedRate(this::onrender, 0, velocity, TimeUnit.MILLISECONDS);
    }

    public void clear(){
        for (int x = 0; x < 50; x++){
            System.out.println(" ");
        }
    }

    /**
     * Exibe o labirinto no console de forma visual
     */
    public void display(boolean on_registered) {
        System.out.println("=== LABIRINTO ===");
        System.out.println("█ = Parede | · = Caminho | * = Início | # = Saída | @ = Mouse");
        System.out.println();

        int index = 0;
        int pos = 0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                boolean is_mouse = i == this.direction_y && j == this.direction_x;

                if (i == startY && j == startX) {
                    if (on_registered) {
                        Mouse mouse = new Mouse(j, i, this); // Corrigido: x,y não y,x
                        Land land = new Land(i, j, index, this);
                        this.entities[pos] = mouse;
                        pos++;
                        this.entities[pos] = land;
                    }
                    System.out.print(is_mouse ? "@ " : "* ");

                } else if (i == endY && j == endX) {
                    if (on_registered) {
                        End end = new End(i, j, this);
                        this.entities[pos] = end;
                    }
                    System.out.print(is_mouse ? "@ " : "# ");

                } else if (matrix[i][j] == WALL) {
                    if (on_registered) {
                        Block block = new Block(i, j, this);
                        this.entities[pos] = block;
                    }
                    System.out.print(is_mouse ? "@ " : "█ ");

                } else {
                    if (on_registered) {
                        index++;
                        Land land = new Land(i, j, index, this);
                        this.entities[pos] = land;
                    }
                    System.out.print(is_mouse ? "@ " : "· ");
                }
                pos++;
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Verifica se uma posição específica é válida para movimento
     */
    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < width &&
                y >= 0 && y < height &&
                matrix[y][x] == PATH;
    }

    /**
     * Verifica se uma posição é o ponto final do labirinto
     */
    public boolean isEndPosition(int x, int y) {
        return x == endX && y == endY;
    }

    // ========== MÉTODOS GETTER ==========
    public int[][] getMatrix() { return matrix; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
    public int getEndX() { return endX; }
    public int getEndY() { return endY; }

    /**
     * Exibe informações técnicas sobre o labirinto
     */
    public void printInfo() {
        System.out.println("Informações do Labirinto:");
        System.out.println("Dimensões: " + width + "x" + height);
        System.out.println("Início: (" + startX + ", " + startY + ")");
        System.out.println("Fim: (" + endX + ", " + endY + ")");
        System.out.println();
    }
}