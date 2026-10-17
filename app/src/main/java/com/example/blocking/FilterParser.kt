package com.example.blocking

object FilterParser {

    fun parseLine(line: String): FilterRule? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("[")) {
            return null // Comment or metadata
        }

        // Cosmetic rules (##) are handled separately by content script
        if (trimmed.contains("##") || trimmed.contains("#@#")) {
            return null
        }

        var work = trimmed
        var ruleType = RuleType.BLOCK

        // Check for exception rule (@@)
        if (work.startsWith("@@")) {
            ruleType = RuleType.EXCEPTION
            work = work.substring(2)
        }

        // Check for options ($)
        var optionsString: String? = null
        if (work.contains("$")) {
            val parts = work.split("$", limit = 2)
            work = parts[0]
            optionsString = parts.getOrNull(1)
        }

        var isDomainAnchor = false
        var pattern = work

        if (pattern.startsWith("||")) {
            isDomainAnchor = true
            pattern = pattern.substring(2)
            if (pattern.endsWith("^")) {
                pattern = pattern.substring(0, pattern.length - 1)
            }
        } else if (pattern.startsWith("|")) {
            pattern = pattern.substring(1)
        }

        if (pattern.endsWith("|")) {
            pattern = pattern.substring(0, pattern.length - 1)
        }

        if (pattern.isEmpty()) {
            return null
        }

        val domainsIncluded = mutableSetOf<String>()
        val domainsExcluded = mutableSetOf<String>()
        val resourceTypes = mutableSetOf<ResourceType>()
        var thirdPartyOnly: Boolean? = null

        if (!optionsString.isNullOrBlank()) {
            val options = optionsString.split(",")
            for (opt in options) {
                val cleanOpt = opt.trim().lowercase()
                when {
                    cleanOpt == "third-party" || cleanOpt == "3p" -> thirdPartyOnly = true
                    cleanOpt == "~third-party" || cleanOpt == "~3p" -> thirdPartyOnly = false
                    cleanOpt == "script" -> resourceTypes.add(ResourceType.SCRIPT)
                    cleanOpt == "image" -> resourceTypes.add(ResourceType.IMAGE)
                    cleanOpt == "stylesheet" || cleanOpt == "css" -> resourceTypes.add(ResourceType.STYLESHEET)
                    cleanOpt == "xmlhttprequest" || cleanOpt == "xhr" -> resourceTypes.add(ResourceType.XMLHTTPREQUEST)
                    cleanOpt == "subdocument" || cleanOpt == "document" -> resourceTypes.add(ResourceType.SUBDOCUMENT)
                    cleanOpt.startsWith("domain=") -> {
                        val domainList = cleanOpt.substring("domain=".length).split("|")
                        for (d in domainList) {
                            if (d.startsWith("~")) {
                                domainsExcluded.add(d.substring(1))
                            } else {
                                domainsIncluded.add(d)
                            }
                        }
                    }
                }
            }
        }

        return FilterRule(
            rawRule = trimmed,
            ruleType = ruleType,
            pattern = pattern,
            isDomainAnchor = isDomainAnchor,
            domainsIncluded = domainsIncluded,
            domainsExcluded = domainsExcluded,
            resourceTypes = resourceTypes,
            thirdPartyOnly = thirdPartyOnly
        )
    }

    fun parseFilterList(content: String): List<FilterRule> {
        val rules = ArrayList<FilterRule>()
        content.lineSequence().forEach { line ->
            parseLine(line)?.let { rules.add(it) }
        }
        return rules
    }
}
