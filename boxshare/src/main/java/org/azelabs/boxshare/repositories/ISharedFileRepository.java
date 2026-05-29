package org.azelabs.boxshare.repositories;

import org.azelabs.boxshare.models.FileModel;
import org.azelabs.boxshare.models.SharedFileModel;
import org.azelabs.boxshare.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ISharedFileRepository extends JpaRepository<SharedFileModel, Long> {
    boolean existsByFileAndUser(FileModel file, UserModel user);
    List<SharedFileModel> findByUser_Identity(UUID identity);
}
