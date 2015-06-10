
withConfig(configuration) {
    inline(phase: 'CONVERSION') { source, context, classNode ->
        classNode.putNodeMetaData('projectVersion', '0.1-SNAPSHOT')
        classNode.putNodeMetaData('projectName', 'grails-api-framework')
        classNode.putNodeMetaData('isPlugin', 'true')
    }
}
