package br.univates.mobile.thiltapes;

public class ItemListaJogos {
    private String titulo, criador, id = null;

    public ItemListaJogos(String titulo, String criador, String id) {
        this.titulo = titulo;
        this.criador = criador;
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getCriador() {
        return criador;
    }

    public String getId() {
        return id;
    }

    public String getDescricao() {
        return String.format("%s ▸ %s", this.criador, this.id);
    }
}