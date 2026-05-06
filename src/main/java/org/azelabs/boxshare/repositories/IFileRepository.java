package org.azelabs.boxshare.repositories;

import org.azelabs.boxshare.models.FileModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFileRepository extends JpaRepository<FileModel, Long> {
    Optional<FileModel> findByName(String name);
}
