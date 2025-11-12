![PLAY](https://github.com/user-attachments/assets/ce5e0307-0710-4b5d-b5a3-44eb0c6a0770)

<img width="996" height="667" alt="image" src="https://github.com/user-attachments/assets/3cc09a79-2df6-4f11-a2fa-4313031db275" />

Space Shooter (Java/Swing)

Joguinho 2D estilo space shooter feito em Java + Swing. Você controla uma nave com turbo, atira em inimigos e precisa desviar de rochas. O cenário tem estrelas animadas, inimigos com respawn e um HUD simples (kills e ticks).

🎮 Gameplay

Player: nave com sprite normal e turbo (muda a velocidade e a imagem).

Tiro: projéteis com cooldown e destruição em colisão.

Inimigos:

Enemy1 / Enemy2: destruíveis com tiro (ou com contato se estiver em turbo).

Enemy3 (rocha): obstáculo — bloqueia tiros e mata ao encostar.

Cenário: fundo estático + estrelas em movimento.

Respawn: quando um inimigo sai da tela/ some, nasce outro automaticamente.

HUD: mostra kills e ticks (frames).


🧱 Organização (arquivos principais)

Fase.java — loop do jogo (update/desenho), spawns, colisões, HUD e input.

Player.java — nave do jogador, turbo, tiro e limites de tela.

Tiro.java — projétil do jogador.

Enemy1.java, Enemy2.java, Enemy3.java — inimigos/obstáculos.

Stars.java — estrelas de fundo.

StartScreen.java (opcional) — tela inicial, se usar.

 Tela de Game Over e pontuação final.

 Sistema de waves/dificuldade progressiva.
