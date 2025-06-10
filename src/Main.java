import maze.*;
import models.Mouse;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SISTEMA DE LABIRINTO COM MÚLTIPLOS RATOS ===\n");

        // Cria o gerador de labirinto
        MazeGenerator generator = new MazeGenerator();

        Scanner scanL1 = new Scanner(System.in);
        Scanner scanL2 = new Scanner(System.in);

        System.out.println("Informe o tamando do labirinto");
        System.out.printf("Largura: ");
        int val1 = scanL1.nextInt();
        System.out.println();
        System.out.printf("Altura: ");
        int val2 = scanL2.nextInt();

        // Gera um labirinto com um tamanho variado onde o usuário escolhe o tamanho
        Maze maze = generator.generateMaze(val1, val2);

        int velocidadeRato = 500;

        Scanner scan = new Scanner(System.in);

        System.out.println("Digite a quantidade de Ratos:");
        int valRato = scan.nextInt();
        // Adiciona a quantidade digitada de ratos
        maze.addMice(valRato, velocidadeRato);

        // Exibe o labirinto inicial
        maze.display();

        // Aguarda um momento para visualizar o estado inicial
        Thread.sleep(2000);

        maze.play(400);

        // Mantém o programa rodando
        Thread.currentThread().join();
    }
}