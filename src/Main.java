import java.util.*;
import maze.*;

public class Main {
    /**
     * Método main - ponto de entrada do programa
     * @param args Argumentos da linha de comando (não utilizados)
     */
    public static void main(String[] args) throws InterruptedException {
        // Título do programa
        System.out.println("=== SISTEMA DE LABIRINTO ===\n");

        // PASSO 1: Criar o gerador de labirintos
        // Esta classe é responsável apenas por gerar, não armazenar
        MazeGenerator generator = new MazeGenerator();

        // PASSO 2: Gerar um labirinto
        // O método generateMaze retorna um objeto Maze pronto para usar
        System.out.println("Gerando labirinto...");
        Maze maze = generator.generateMaze(35, 35); // Cria labirinto 15x15

        // PASSO 3: Exibir informações do labirinto gerado
        // Mostra dimensões e coordenadas importantes
        maze.printInfo();

        // PASSO 4: Exibir o labirinto visualmente
        // Mostra o labirinto com símbolos no console
        maze.display(true);
        maze.play(250);

        // PASSO 5: Exemplo de como acessar dados do labirinto
        // Estes dados podem ser usados para implementar um jogo
        System.out.println("=== EXEMPLO DE USO DOS DADOS ===");

        // Obtém coordenadas importantes
        System.out.println("Posição inicial: (" + maze.getStartX() + ", " + maze.getStartY() + ")");
        System.out.println("Posição final: (" + maze.getEndX() + ", " + maze.getEndY() + ")");

        // Exemplo de verificação de posição válida
        boolean validStart = maze.isValidPosition(maze.getStartX(), maze.getStartY());
        System.out.println("Posição inicial é válida? " + validStart);

//        // Exemplo de verificação de diferentes posições
//        System.out.println("Posição (0,0) é válida? " + maze.isValidPosition(0, 0)); // Provavelmente false (parede)
//        System.out.println("Posição (1,1) é válida? " + maze.isValidPosition(1, 1)); // Provavelmente true (caminho)
//
//        // EXEMPLO: Criar múltiplos labirintos
//        System.out.println("\n=== SEGUNDO LABIRINTO (menor) ===");
//        Maze maze2 = generator.generateMaze(11, 11); // Labirinto menor
//        maze2.display(true);
//
//        // EXEMPLO: Demonstrar como acessar a matriz diretamente
//        System.out.println("=== EXEMPLO DE ACESSO À MATRIZ ===");
//        int[][] matriz = maze.getMatrix(); // Obtém a matriz do labirinto
//
//        // Exemplo de como você poderia usar isso em um jogo:
//        System.out.println("Valor na posição inicial: " + matriz[maze.getStartY()][maze.getStartX()]);
//        System.out.println("(0 = parede, 1 = caminho)");

        // Exemplo de loop para encontrar todos os caminhos livres
//        System.out.println("\nContando caminhos livres...");
//        int caminhos = 0;
//        for (int y = 0; y < maze.getHeight(); y++) {
//            for (int x = 0; x < maze.getWidth(); x++) {
//                if (matriz[y][x] == Maze.PATH) { // Usa constante da classe Maze
//                    caminhos++;
//                }
//            }
//        }
//        System.out.println("Total de células de caminho: " + caminhos);
    }
}