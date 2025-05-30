package maze;

import java.util.*;

public class MazeGenerator {
    // Constantes para representar os tipos de célula na matriz
    private static final int WALL = 0;      // Valor 0 = Parede
    private static final int PATH = 1;      // Valor 1 = Caminho livre

    // Array bidimensional que define as 4 direções possíveis de movimento
    // Cada sub-array contém [deltaY, deltaX] para mover 2 células por vez
    private static final int[][] DIRECTIONS = {
            {-2, 0},  // Norte: move 2 posições para cima (Y diminui)
            {2, 0},   // Sul: move 2 posições para baixo (Y aumenta)
            {0, 2},   // Leste: move 2 posições para direita (X aumenta)
            {0, -2}   // Oeste: move 2 posições para esquerda (X diminui)
    };

    // Gerador de números aleatórios para criar variação nos labirintos
    private Random random;

    /**
     * Construtor da classe MazeGenerator
     * Inicializa o gerador de números aleatórios
     */
    public MazeGenerator() {
        this.random = new Random(); // Cria novo objeto Random para gerar números aleatórios
    }

    /**
     * Método principal que gera um novo labirinto
     * @param width Largura desejada (será convertida para ímpar se necessário)
     * @param height Altura desejada (será convertida para ímpar se necessário)
     * @return Objeto Maze contendo o labirinto gerado
     */
    public Maze generateMaze(int width, int height) {
        // Garante que as dimensões sejam ímpares (algoritmo funciona melhor assim)
        // Se for par, adiciona 1 para tornar ímpar
        int finalWidth = width % 2 == 0 ? width + 1 : width;
        int finalHeight = height % 2 == 0 ? height + 1 : height;

        // Cria a matriz bidimensional que representará o labirinto
        // [linha][coluna] onde linha = Y e coluna = X
        int[][] mazeMatrix = new int[finalHeight][finalWidth];

        // Passo 1: Inicializa toda a matriz com paredes
        initializeMaze(mazeMatrix, finalHeight, finalWidth);

        // Passo 2: Gera os caminhos usando recursão e backtracking
        // Começa na posição (1,1) que é sempre um ponto válido
        generateMazeRecursive(mazeMatrix, 1, 1, finalWidth, finalHeight);

        // Passo 3: Cria pontos de entrada e saída do labirinto
        createEntranceAndExit(mazeMatrix, finalHeight, finalWidth);

        // Passo 4: Retorna um objeto Maze com o labirinto pronto
        return new Maze(mazeMatrix, finalWidth, finalHeight);
    }

    /**
     * Inicializa toda a matriz do labirinto com paredes
     * @param maze Matriz a ser inicializada
     * @param height Altura da matriz
     * @param width Largura da matriz
     */
    private void initializeMaze(int[][] maze, int height, int width) {
        // Loop duplo para percorrer todas as posições da matriz
        for (int i = 0; i < height; i++) {        // Loop das linhas (Y)
            for (int j = 0; j < width; j++) {     // Loop das colunas (X)
                maze[i][j] = WALL; // Define cada célula como parede inicialmente
            }
        }
    }

    /**
     * Método recursivo que gera os caminhos do labirinto usando backtracking
     * @param maze Matriz do labirinto
     * @param x Posição X atual (coluna)
     * @param y Posição Y atual (linha)
     * @param width Largura total da matriz
     * @param height Altura total da matriz
     */
    private void generateMazeRecursive(int[][] maze, int x, int y, int width, int height) {
        // Marca a posição atual como caminho (remove a parede)
        maze[y][x] = PATH;

        // Cria uma lista com todas as direções possíveis
        List<int[]> directions = new ArrayList<>(Arrays.asList(DIRECTIONS));

        // Embaralha as direções para criar aleatoriedade no labirinto
        Collections.shuffle(directions, random);

        // Tenta cada direção em ordem aleatória
        for (int[] direction : directions) {
            // Calcula nova posição baseada na direção atual
            int newX = x + direction[1]; // Nova coordenada X (coluna)
            int newY = y + direction[0]; // Nova coordenada Y (linha)

            // Verifica se a nova posição é válida e ainda é uma parede
            if (isValidCell(newX, newY, width, height) && maze[newY][newX] == WALL) {

                // Calcula a posição da parede entre a célula atual e a nova
                // Divide por 2 porque nos movemos 2 células por vez
                int wallX = x + direction[1] / 2; // X da parede intermediária
                int wallY = y + direction[0] / 2; // Y da parede intermediária

                // Remove a parede intermediária (cria conexão)
                maze[wallY][wallX] = PATH;

                // Chama recursivamente para continuar gerando a partir da nova posição
                generateMazeRecursive(maze, newX, newY, width, height);
            }
        }
    }

    /**
     * Verifica se uma célula está dentro dos limites válidos do labirinto
     * @param x Coordenada X a verificar
     * @param y Coordenada Y a verificar
     * @param width Largura total da matriz
     * @param height Altura total da matriz
     * @return true se a posição é válida, false caso contrário
     */
    private boolean isValidCell(int x, int y, int width, int height) {
        // Verifica se está dentro das bordas, deixando 1 célula de margem
        return x >= 1 && x < width - 1 && y >= 1 && y < height - 1;
    }

    /**
     * Cria pontos de entrada e saída no labirinto
     * @param maze Matriz do labirinto
     * @param height Altura da matriz
     * @param width Largura da matriz
     */
    private void createEntranceAndExit(int[][] maze, int height, int width) {
        // Entrada: primeira linha, segunda coluna [0][1]
        maze[0][1] = PATH;

        // Saída: última linha, penúltima coluna [height-1][width-2]
        maze[height - 1][width - 2] = PATH;
    }
}
