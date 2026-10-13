package de.TeutonStudio.MathematikRechenSystem.kern

/** Radikaldarstellungen mit expliziter Zweigkopplung; keine Unlösbarkeitsaussage bei höherem Grad. */
fun analytischePolynomFormeln(problem:PolynomGleichungsProblem):List<String> {
    if(problem.variablen.size!=1 || problem.komponenten.size!=1) return emptyList()
    val v=problem.variablen.single()
    val p=zerlegeAlsPolynom(problem.komponenten.single(),v)?:return emptyList()
    val k=p.koeffizienten.map { KomplexRational.von(it)?:return emptyList() }.dropLastWhile { it.istNull() }
    val n=k.lastIndex
    if(n<1) return emptyList()
    val a=k.map { it/k.last() }
    fun l(z:KomplexRational)=z.alsAusdruck().zuLatex()
    val x=v.zuLatex()
    if(n==1) return listOf("$x=${l(-a[0])}")
    if(n==2) {
        val d=a[1]*a[1]-KomplexRational(pQ(4))*a[0]
        return listOf("$x=\\frac{-(${l(a[1])})\\pm\\sqrt{${l(d)}}}{2}")
    }
    if(n==3) {
        val b=a[2]; val pp=a[1]-b*b/KomplexRational(pQ(3))
        val qq=KomplexRational(pQ(2))*b*b*b/KomplexRational(pQ(27))-b*a[1]/KomplexRational(pQ(3))+a[0]
        return listOf("$x=t-\\frac{${l(b)}}{3}","t^3+(${l(pp)})t+(${l(qq)})=0",
            "u^3=-\\frac{${l(qq)}}{2}+\\sqrt{\\left(\\frac{${l(qq)}}{2}\\right)^2+\\left(\\frac{${l(pp)}}{3}\\right)^3}",
            "v^3=-\\frac{${l(qq)}}{2}-\\sqrt{\\left(\\frac{${l(qq)}}{2}\\right)^2+\\left(\\frac{${l(pp)}}{3}\\right)^3}",
            "uv=-\\frac{${l(pp)}}{3},\\quad t=u+v")
    }
    if(n==4) {
        val b=a[3]; val pp=a[2]-KomplexRational(pQ(3))*b*b/KomplexRational(pQ(8))
        val qq=a[1]-b*a[2]/KomplexRational(pQ(2))+b*b*b/KomplexRational(pQ(8))
        val rr=a[0]-b*a[1]/KomplexRational(pQ(4))+b*b*a[2]/KomplexRational(pQ(16))-KomplexRational(pQ(3))*b*b*b*b/KomplexRational(pQ(256))
        val basis=listOf("$x=t-\\frac{${l(b)}}{4}","t^4+(${l(pp)})t^2+(${l(qq)})t+(${l(rr)})=0")
        return if(qq.istNull()) basis+"t^2=\\frac{-(${l(pp)})\\pm\\sqrt{(${l(pp)})^2-4(${l(rr)})}{2}"
        else basis+listOf("4(2m-(${l(pp)}))(m^2-(${l(rr)}))-(${l(qq)})^2=0",
            "u^2=2m-(${l(pp)}),\\quad u\\ne0",
            "t^2-ut+m+\\frac{${l(qq)}}{2u}=0\\quad\\lor\\quad t^2+ut+m-\\frac{${l(qq)}}{2u}=0")
    }
    return listOf("P($x)=0,\\quad \\deg P=$n")
}

/** Faddeev–LeVerrier über Q(i); dieselben Eigenwerte wie det(A−λI). */
fun komplexesCharakteristischesPolynom(matrix:Matrix,budget:PolynomRechenBudget=PolynomRechenBudget()):List<KomplexRational> {
    require(matrix.zeilenAnzahl==matrix.spaltenAnzahl && matrix.zeilenAnzahl in 1..8)
    val n=matrix.zeilenAnzahl
    val a=matrix.zeilen.map { row -> row.map { KomplexRational.von(it)?:error("Eigenwertnachweise benötigen rational-komplexe Einträge.") } }
    var b=List(n) { i -> List(n) { j -> if(i==j) kEins else kNull } }
    val absteigend=mutableListOf(kEins)
    for(k in 1..n) {
        val ab=List(n) { i -> List(n) { j -> (0 until n).fold(kNull) { s,h ->
            budget.schritt(); (s+a[i][h]*b[h][j]).also { budget.zahl(it.re); budget.zahl(it.im) }
        } } }
        val c=-(0 until n).fold(kNull) { s,i -> s+ab[i][i] }/KomplexRational(pQ(k))
        absteigend+=c
        b=List(n) { i -> List(n) { j -> ab[i][j]+if(i==j) c else kNull } }
    }
    return absteigend.reversed()
}
fun eigenwertPolynomProblem(matrix:Matrix,budget:PolynomRechenBudget=PolynomRechenBudget()):PolynomGleichungsProblem {
    val v=Variable("lambda","\\lambda")
    val p=polynomAusKoeffizienten(komplexesCharakteristischesPolynom(matrix,budget).map { it.alsAusdruck() },v)
    return PolynomGleichungsProblem(Gleichheit(p,pNull),listOf(v),listOf(p),true)
}

/** Leichte Anfrage; das charakteristische Polynom entsteht erst im Hintergrunddienst. */
fun eigenwertAnfrage(matrix:Matrix):PolynomGleichungsProblem? {
    if(matrix.zeilenAnzahl!=matrix.spaltenAnzahl || matrix.zeilenAnzahl !in 1..8 ||
        matrix.zeilen.flatten().any { KomplexRational.von(it)==null }) return null
    return PolynomGleichungsProblem(Gleichheit(AllgemeinerParameter("chi","\\chi_A(\\lambda)"),pNull),
        listOf(Variable("lambda","\\lambda")),emptyList(),true,eigenwertMatrix=matrix)
}
