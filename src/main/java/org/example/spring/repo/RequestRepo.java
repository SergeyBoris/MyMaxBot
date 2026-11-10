package org.example.spring.repo;

import org.example.entity.Contragents.Contragent;
import org.example.entity.Req;
import org.example.entity.ReqStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface  RequestRepo extends JpaRepository<Req, Integer> {
    Optional<Req> findByRequestNumber(String requestNumber);

    // С оборудованием (JOIN FETCH) — чтобы не было LazyInitializationException
    @Query("SELECT DISTINCT r FROM Req r LEFT JOIN FETCH r.equipment WHERE r.requestNumber = :n")
    Optional<Req> findByRequestNumberWithEquipment(@Param("n") String number);

    // История по серийнику
    @Query("SELECT DISTINCT r FROM Req r JOIN FETCH r.equipment e " +
            "WHERE e.serialNumber = :sn ORDER BY r.creationTime DESC")
    List<Req> findHistoryBySerial(@Param("sn") String serial);

    @Query("SELECT r.requestNumber FROM Req r")
    Set<String> findAllRequestNumbers();

    @Query("SELECT r.requestNumber FROM Req r WHERE r.status IN :statuses")
    Set<String> findAllRequestNumbersByStatuses(@Param("statuses") Collection<ReqStatus> statuses);

    @Query("SELECT r FROM Req r WHERE r.contragent = :c " +
            "AND r.status IN :statuses ORDER BY r.sla DESC NULLS LAST")
    List<Req> findByContragentAndStatuses(@Param("c") String c,
                                          @Param("statuses") Collection<ReqStatus> statuses);

}
