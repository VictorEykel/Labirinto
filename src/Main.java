import maze.*;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SISTEMA DE LABIRINTO COM MÚLTIPLOS RATOS ===\n");

        // Cria o gerador de labirinto
        MazeGenerator generator = new MazeGenerator();

        // Gera um labirinto 21x21 (tamanho maior para mais espaço)
        Maze maze = generator.generateMaze(15, 15);

        int velocidadeRato = 500;

        Scanner scan = new Scanner(System.in);

        System.out.println("Digite a quantidade de Ratos:");
        int valRato = scan.nextInt();
        // Adiciona múltiplos ratos ao labirinto
        maze.addMice(valRato, velocidadeRato); // Adiciona 5 ratos

        // Exibe o labirinto inicial
        maze.display();

        // Aguarda um momento para visualizar o estado inicial
        Thread.sleep(2000);

        maze.play(400);

        // Mantém o programa rodando
        Thread.currentThread().join();
    }
}