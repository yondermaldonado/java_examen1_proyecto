package com.carmotors.dao;

import com.carmotors.modelo.ServicioCatalogo;
import java.sql.Connection;
import java.util.List;

public interface ServicioCatalogoDAO {
    List<ServicioCatalogo> listar(Connection con);
}
