package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Auditoria;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.Repository;

import java.util.List;

// PARTE 2: AuditoriaRepository agora estende Repository, e não mais MongoRepository
// MongoRepository trazia prontos os métodos delete, deleteAll e deleteById,
// então qualquer classe do projeto conseguiria apagar um registro de auditoria.
// Repository é uma interface vazia: só existem os métodos que nós declaramos aqui embaixo.
// Assim a auditoria só aceita INCLUIR e CONSULTAR, como pede o RF05
// ("não permitir alteração ou exclusão dos registros").
public interface AuditoriaRepository extends Repository<Auditoria, String> {

    // Grava um novo registro de auditoria (usado pelo AuditoriaService)
    Auditoria save(Auditoria auditoria);

    // Lista todos os registros na ordem pedida (o AuditoriaService pede do mais recente para o mais antigo)
    List<Auditoria> findAll(Sort sort);

    // Busca todos os registros de auditoria de um usuário específico
    // Exemplo: ver tudo que o usuário "Paulo Silva" fez no sistema
    List<Auditoria> findByUsuarioResponsavel(String usuarioResponsavel);

    // Busca todos os registros de um tipo de ação específica
    // Exemplo: ver todos os "PEDIDO_CANCELADO" do sistema
    List<Auditoria> findByAcao(String acao);
}

//PARTE 1: AuditoriaRepository herdava os métodos prontos de MongoRepository (save, findAll, findById, delete).
//MongoRepository não é criado no projeto, vem da biblioteca spring-data-mongodb do pom.xml.
//PARTE 2: agora herda de Repository, que também vem do spring-data, mas não traz nenhum método pronto.
//Por isso save e findAll(Sort) foram declarados aqui, com o mesmo nome e parâmetros do MongoRepository,
//e o Spring usa a implementação pronta deles. delete não foi declarado, então não existe.
//findByUsuarioResponsavel e findByAcao são métodos extras que não são
//herdados pelo MongoRepository, por isso devem ser criados nesta interface.

//Os métodos desta interface não tem corpo, pois o Spring os implementa
//automaticamente em tempo de execução, lendo o nome do metodo, formado
//por findBy + nomeAtributo, e gerando a query correspondente no banco de dados.
//Exemplo: metodo findByUsuarioResponsavel → procura o atributo usuarioResponsavel na
//classe Auditoria → gera automaticamente a query WHERE usuarioResponsavel = ?.
//Se o nome não bater com o atributo, dá erro na inicialização.
