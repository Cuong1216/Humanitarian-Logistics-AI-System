package com.humanitarian.logistics.core.logistics.repository;

import com.humanitarian.logistics.core.logistics.dto.RouteStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class PgRoutingRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PgRoutingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes the pgRouting Dijkstra algorithm between two GPS points.
     * Note: This method relies on PostGIS and pgRouting extensions being installed.
     */
    public List<RouteStep> findShortestPath(double startLat, double startLng, double endLat, double endLng) {
        String sql = """
            WITH start_node AS (
                SELECT id FROM road_network_vertices_pgr
                ORDER BY the_geom <-> ST_SetSRID(ST_MakePoint(?, ?), 4326)
                LIMIT 1
            ),
            end_node AS (
                SELECT id FROM road_network_vertices_pgr
                ORDER BY the_geom <-> ST_SetSRID(ST_MakePoint(?, ?), 4326)
                LIMIT 1
            )
            SELECT 
                r.seq, 
                r.node, 
                r.edge, 
                r.cost AS node_cost, 
                COALESCE(rd.is_flooded, FALSE) AS is_flooded,
                ST_AsText(rd.geom) AS path_wkt
            FROM pgr_dijkstra(
                'SELECT 
                    id, 
                    source, 
                    target, 
                    CASE 
                        WHEN is_flooded THEN cost * 10.0 
                        ELSE cost 
                    END AS cost,
                    CASE 
                        WHEN is_flooded THEN reverse_cost * 10.0 
                        ELSE reverse_cost 
                    END AS reverse_cost
                 FROM road_network',
                (SELECT id FROM start_node), 
                (SELECT id FROM end_node), 
                directed := true
            ) AS r
            LEFT JOIN road_network AS rd ON r.edge = rd.id
            ORDER BY r.seq
            """;

        return jdbcTemplate.query(
            sql,
            new Object[]{startLng, startLat, endLng, endLat}, // PostGIS uses Longitude, Latitude order
            new RowMapper<RouteStep>() {
                @Override
                public RouteStep mapRow(ResultSet rs, int rowNum) throws SQLException {
                    RouteStep step = new RouteStep();
                    step.setSeq(rs.getInt("seq"));
                    step.setNode(rs.getLong("node"));
                    step.setEdge(rs.getLong("edge"));
                    step.setCost(rs.getDouble("node_cost"));
                    step.setFlooded(rs.getBoolean("is_flooded"));
                    step.setPathWkt(rs.getString("path_wkt"));
                    return step;
                }
            }
        );
    }
}
