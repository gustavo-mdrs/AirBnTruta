package com.devcaotics.airBnTruta.model.repositories;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.devcaotics.airBnTruta.model.entities.Interesse;

public final class InteresseRepository implements Repository<Interesse,Integer>{

    protected InteresseRepository(){}

    @Override
    public void create(Interesse i) throws SQLException {
        String sql = "INSERT INTO interesse (realizado, proposta, tempo_permanencia, fugitivo_id, hospedagem_id) "
                   + "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement stmt = ConnectionManager.getCurrentConnection().prepareStatement(sql);
        stmt.setLong(1, i.getRealizado());
        stmt.setString(2, i.getProposta());
        stmt.setInt(3, i.getTempoPermanencia());
        stmt.setInt(4, i.getInteressado().getCodigo());
        stmt.setInt(5, i.getInteresse().getCodigo());

        stmt.execute();
    }

    @Override
    public void update(Interesse c) throws SQLException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public Interesse read(Integer k) throws SQLException {
        String sql = "SELECT * FROM interesse WHERE codigo = ?";
        
        PreparedStatement stmt = ConnectionManager.getCurrentConnection().prepareStatement(sql);
        stmt.setInt(1, k);
        
        ResultSet rs = stmt.executeQuery();
        
        if (!rs.next()) {
            return null;
        }
        
        Interesse i = new Interesse();
        i.setCodigo(rs.getInt("codigo"));
        i.setRealizado(rs.getLong("realizado"));
        i.setProposta(rs.getString("proposta"));
        i.setTempoPermanencia(rs.getInt("tempo_permanencia"));
        
        i.setInteressado(new FugitivoRepository().read(rs.getInt("fugitivo_id")));
        i.setInteresse(new HospedagemRepository().read(rs.getInt("hospedagem_id")));
        
        return i;
    }

    @Override
    public void delete(Integer k) throws SQLException {
        String sql = "DELETE FROM interesse WHERE codigo = ?";
        
        PreparedStatement stmt = ConnectionManager.getCurrentConnection().prepareStatement(sql);
        stmt.setInt(1, k);
        stmt.execute();
    }

    @Override
    public List<Interesse> readAll() throws SQLException {
        String sql = "SELECT * FROM interesse ORDER BY realizado DESC";
        
        PreparedStatement stmt = ConnectionManager.getCurrentConnection().prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        
        List<Interesse> interesses = new ArrayList<>();
        
        while (rs.next()) {
            Interesse i = new Interesse();
            i.setCodigo(rs.getInt("codigo"));
            i.setRealizado(rs.getLong("realizado"));
            i.setProposta(rs.getString("proposta"));
            i.setTempoPermanencia(rs.getInt("tempo_permanencia"));
            
            // Carregar relacionamentos
            i.setInteressado(new FugitivoRepository().read(rs.getInt("fugitivo_id")));
            i.setInteresse(new HospedagemRepository().read(rs.getInt("hospedagem_id")));
            
            interesses.add(i);
        }
        
        return interesses;
    }

    public List<Interesse> filterByFugitivo(int fugitivoId) throws SQLException {
        String sql = "SELECT i.* FROM interesse i " +
                 "INNER JOIN hospedagem h ON i.hospedagem_id = h.codigo " +
                 "WHERE i.fugitivo_id = ? " +  
                 "ORDER BY i.realizado DESC";
        
        PreparedStatement stmt = ConnectionManager.getCurrentConnection().prepareStatement(sql);
        stmt.setInt(1, fugitivoId);
        
        ResultSet rs = stmt.executeQuery();
        List<Interesse> interesses = new ArrayList<>();
        
        while (rs.next()) {
            Interesse i = new Interesse();
            i.setCodigo(rs.getInt("codigo"));
            i.setRealizado(rs.getLong("realizado"));
            i.setProposta(rs.getString("proposta"));
            i.setTempoPermanencia(rs.getInt("tempo_permanencia"));
            
            i.setInteressado(new FugitivoRepository().read(rs.getInt("fugitivo_id")));
            i.setInteresse(new HospedagemRepository().read(rs.getInt("hospedagem_id")));
            
            interesses.add(i);
        }
        
        return interesses;
    }

    public List<Interesse> filterByHospedagem(int hospedagemId) throws SQLException {
        String sql = "SELECT * FROM interesse WHERE hospedagem_id = ? ORDER BY realizado DESC";
        
        PreparedStatement stmt = ConnectionManager.getCurrentConnection().prepareStatement(sql);
        stmt.setInt(1, hospedagemId);
        
        ResultSet rs = stmt.executeQuery();
        List<Interesse> interesses = new ArrayList<>();
        
        while (rs.next()) {
            Interesse i = new Interesse();
            i.setCodigo(rs.getInt("codigo"));
            i.setRealizado(rs.getLong("realizado"));
            i.setProposta(rs.getString("proposta"));
            i.setTempoPermanencia(rs.getInt("tempo_permanencia"));
            
            i.setInteressado(new FugitivoRepository().read(rs.getInt("fugitivo_id")));
            i.setInteresse(new HospedagemRepository().read(rs.getInt("hospedagem_id")));
            
            interesses.add(i);
        }
        
        return interesses;
    }

    public boolean hasInterest(int fugitivoId, int hospedagemId) throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM interesse " +
                     "WHERE fugitivo_id = ? AND hospedagem_id = ?";
        
        PreparedStatement stmt = ConnectionManager.getCurrentConnection().prepareStatement(sql);
        stmt.setInt(1, fugitivoId);
        stmt.setInt(2, hospedagemId);
        
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("total") > 0;
        }
        
        return false;
    }

    public int countByHospedagem(int hospedagemId) throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM interesse WHERE hospedagem_id = ?";
        
        PreparedStatement stmt = ConnectionManager.getCurrentConnection().prepareStatement(sql);
        stmt.setInt(1, hospedagemId);
        
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("total");
        }
        
        return 0;
    }
}