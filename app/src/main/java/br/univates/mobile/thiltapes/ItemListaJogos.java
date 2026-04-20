package br.univates.mobile.thiltapes;

import androidx.annotation.NonNull;

/**
 * Item de lista de jogo ({@code GET /jogos}): id e nome.
 */
public class ItemListaJogos {

    private final int id;
    @NonNull
    private final String nome;

    public ItemListaJogos(int id, @NonNull String nome) {
        this.id = id;
        this.nome = nome;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getNome() {
        return nome;
    }

    @NonNull
    public String getDescricao() {
        return "ID " + id;
    }
}
