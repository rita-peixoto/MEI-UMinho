package DAO;

import java.util.List;

public interface Dados<K> {
    Dados<K> fromRow(List<String> row);

    List<String> toRow();
}