
# Kompresi Gambar dengan Quad Tree
**Quad Tree** adalah struktur data rekursif berbentuk pohon yang digunakan untuk membagi ruang dua dimensi (2D) menjadi empat kuadran secara hierarkis. Pada setiap simpul, ruang dibagi menjadi empat bagian yang lebih kecil: Top-Left, Top-Right, Bottom-Left, dan Bottom-Right. Proses ini dilakukan secara rekursif hanya jika suatu bagian tidak memenuhi kondisi homogenitas tertentu (misalnya terlalu banyak variasi warna).

![Alt Text](test/eksperimen1/solutions/output.gif)

## Kontributor
| NIM       | Nama |
|------------------|-------------|
| 13523045        | Nadhif Radityo Nugroho|
|       13523052   | Adhimas Aryo Bimo |

## Instalasi

Untuk melakukan instalasi pada project ini, Anda diperlukan untuk memiliki Java dengan SDK 8 atau versi terbaru dan memiliki IDE yang mendukung bahasa Java.

Setelah melakukannya, Anda dapat melakukan clone pada repository ini dengan mengikuti command berikut:

```bash
  git https://github.com/ryonlunar/Tucil2_13523045_13523052
  cd Tucil2_13523045_13523052
```

    
## Cara Menjalankan

Untuk menjalankan program anda memerlukan beberapa command seperti berikut:

```
 -b,--block <arg>       Minimum block size
 -g,--gif               Generate gif animation
 -h,--help              Shows help message
 -i,--input <arg>       Input image file
 -m,--method <arg>      QuadTree compression method
 -o,--output <arg>      Output image file
 -t,--threshold <arg>   QuadTree compression threshold
    --verbose           Output verbose
```
Dengan contoh menjalankan program seperti berikut:

```
java -jar build/libs/Tucil2_13523045_13523052.jar -i (path_gambar) -m (methode) -t (threshold) -b (minblocksize) -o (folder_output) -g (jika ingin memberikan output gif)
```
Terdapat beberapa methode yang dapat digunakan seperti 
Variance,
		MeanAbsoluteDeviation,
		MaxPixelDifference,
		Entropy,
		SSIM.
Diharapkan untuk mengisi metode sesuai dengan case tersebut (case sensitive).

Berikut merupakan contoh penggunaan program:
```
java -jar build/libs/Tucil2_13523045_13523052.jar -i test/eksperimen3/nadhif.png -m MeanAbsoluteDeviation -t 20 -b 8 -o test/eksperimen3/solutions -g
```



## Data Structure

``` 
Tucil2_13523045_13523052/
├── README.md                      # Deskripsi dan petunjuk penggunaan program
├── build/                         # Hasil kompilasi Gradle
│   ├── classes/                  # File .class hasil kompilasi
│   └── libs/                     # File .jar hasil build
│       └── Tucil2_13523045_13523052.jar
├── build.gradle.kts              # Konfigurasi build Gradle (Kotlin DSL)
├── docs/                         # Dokumentasi tugas
│   ├── Tucil2-Stima-2025.pdf
│   └── Tucil2_13523045_13523052.pdf
├── gradle/                       # Konfigurasi internal Gradle
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradle.properties             # Properti global Gradle
├── gradlew                       # Script build Gradle (Unix)
├── gradlew.bat                   # Script build Gradle (Windows)
├── settings.gradle.kts           # Setting proyek Gradle
├── src/                          # Kode sumber program
│   ├── main/
│   │   └──java/
│   │       └── tucil2_13523045_13523052/
│   │           ├── Boundary2DQuadTree.java
│   │           ├── GifSequenceWriter.java
│   │           ├── ImageBuffer.java
│   │           ├── ImageQuadTreeCompressor.java
│   │           ├── ImageStatistics.java
│   │           ├── Main.java
│   │           └── Utils.java
│   │  
│   └── test/
│       ├── java/
│       │   └── tucil2_13523045_13523052/
│       │       └── test/       
│       └── resources/
├── test/                         
│   ├── eksperimen1/
│   ├── eksperimen2/
│   ├── eksperimen3/
│   ├── eksperimen4/
│   ├── eksperimen5/
│   ├── eksperimen6/
│   └── eksperimen7/
├── .gitignore                    # File untuk mengecualikan file saat commit
├── .gitattributes                # Konfigurasi git tambahan
└── .vscode/      


```


## Lessons Learned

Dalam projek ini kami belajar untuk mengimplementasikan algoritma Divide and Conquer dalam mengatasi persoalan untuk melakukan kompresi gambar. Kami juga berhasil melakukan analisis kompleksitas waktu serta eksperimen dalam program ini.

## ✅ Evaluasi Fungsionalitas Program

| No. | Poin Evaluasi                                                                 | Ya ✅ | Tidak ❌ |
|-----|-------------------------------------------------------------------------------|:----:|:--------:|
| 1   | Program berhasil dikompilasi tanpa kesalahan                                 | ✅   |          |
| 2   | Program berhasil dijalankan                                                  | ✅   |          |
| 3   | Program berhasil melakukan kompresi gambar sesuai parameter yang ditentukan | ✅   |          |
| 4   | Mengimplementasi seluruh metode perhitungan error wajib                      | ✅   |          |
| 5   | **[Bonus]** Implementasi persentase kompresi sebagai parameter tambahan      | ❌   | ✅        |
| 6   | **[Bonus]** Implementasi Structural Similarity Index (SSIM) sebagai metode pengukuran error | ✅   |          |
| 7   | **[Bonus]** Output berupa GIF Visualisasi Proses pembentukan Quadtree dalam Kompresi Gambar | ✅   |          |
| 8   | Program dan laporan dibuat (kelompok) sendiri                                | ✅   |          |



## License

[MIT](https://choosealicense.com/licenses/mit/)

