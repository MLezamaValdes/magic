!#########################################################################################
!!
!!  NAME
!!  Program "RadarBin2Rst"
!!
!!  PURPOSE
!!  Ein Radolan-RW-Datensatz wird eingelesen und verarbeitet
!!
!! Der RW-Datensatz besteht aus einem variablen Header und 900*900 Datensaetzen
!! zu je 2 Byte.
!! Der Header besteht aus ASCII-Zeichen und ist variablen, weil die Kuerzel der
!! Radarstandorte, die zum Aufbau des Komposit-Bildes verwendet wurden, 
!! aufgelistet sind : bei 16 Radar-Standorten ist der Header maximal 138 Bytes
!! lang. Jedes Radar-Kürzel besteht aus 4 Zeichen : ",xxx", sodass der Header
!! um n*4 Bytes kürzer sein kann, wenn n Radarstandorte gefehlt haben.
!!
!! Der RW-File wird als form='unformatted',access='direct',recl=1620146 einge-
!! lesen (falls die Recordlänge wegen eines kuerzeren Headers nicht so lang ist,
!! gibt es keine Probleme).
!!
!! Der Datensatz wird in das byte-Array buffer(1620146) eingelesen.
!! Das Ende des Headers (Char(3):"etx") wird gesucht.
!! Ausgabe des Headers (nach File Header) und seiner Laenge.
!! Da der Beginn des Datensatzes ueber das Ende des Headers ("etx") gesucht wird,
!! funktioniert das Programm auch, wenn die RW-Daten aus der PEGASUS-Datenbank
!! geholt werden und noch 8 Bytes vor dem eigentlichen Header haben.
!!
!! Das byte-Array buffer ist mit dem integer*2-Array w_buffer(810074) equivalent
!! gesetzt. Die Zahlenwerte stehen somit ab Header-Ende in w_buffer (in 1/10mm).
!!
!! Die Werte der Radar-Pixel bestehen aus 2 Bytes in "little endian"-Kordierung
!! Die Werte umfassen 12 Bits : Wert = 1 in 12 Bit-Darstellung :
!! 0000 0000 0001 --> in 16 Bit 0000 0000 0000 0001
!! in little-endian auf 2 Byte  0000 0001 0000 0000
!!
!! Entsprechend Fehlkennung (Wert 2500 incl. Fehlkennungsflag) :
!! 0010 1001 1100 0100 --> in little-endian auf 2 Byte 1100 0100 0010 1001
!!      DATA wfehl/B"1100010000101001"/
!! (nur diese Fehlkennung pruefe ich z.Z. ab !!)

!! Die Daten-Bytes werden in das Integer-Array obsfield(900*900) eingespeichert.
!! Fehlkennung in obsfield ist -100.
!!
!!
!!  CALLING SEQUENCE
!!  gfortran -fno-range-check RadarBin2Rst.f90 -o RadarBin2Rst.x
!!
!!
!!  COMMENT
!!  - none
!!                                                                                       
!!
!!  LANGUAGE
!!  FORTRAN 90/95
!!
!!  CONTACT
!!  Please send any comments, suggestions, criticism, or (for our sake) bug reports to
!!  kuehnlei@staff.uni-marburg.de 
!!
!!
!!  This program is free software without any warranty and without even the implied 
!!  warranty of merchantability or fitness for a particular purpose.
!!  If you use the software you must acknowledge the software and its author.
!!
!!
!!  HISTORY
!!
!!
!#########################################################################################

    program RadarBin2Rst

!*****************************************************************************************
!
!	Declaration of variables
!
!*****************************************************************************************

  implicit none
    

  INTEGER(2) :: iCounter

  LOGICAL(4) :: bError      !! Error indicator

  CHARACTER(300) :: chFileInput, chFileOutput
  CHARACTER(10) :: chCols    !! Command line argument liCols
  CHARACTER(10) :: chRows    !! Command line argument for liRows

  INTEGER(4) :: liCols !! Number of columns in binary input files
  INTEGER(4) :: liRows !! Number of rows in binary input files

  CHARACTER(2) :: chYear
  CHARACTER(2) :: chMonth
  CHARACTER(2) :: chDay
  CHARACTER(2) :: chHour
  CHARACTER(2) :: chMinute  
  CHARACTER(12) :: chDate
  CHARACTER(30) :: chNameFiletitle
  INTEGER(2) ::  iDatatype       !! Datatype of Idrisi file (1:byte 2:integer*2 4:real*4)
  INTEGER(4) :: liReclReal !! Recordlength for satellite data binary input files
  INTEGER :: iidim, jidim, x, y


!! -- Aus dem DWD-RW ----------------------------------------------------------------------
      INTEGER i,j,k,l,irm,n,m,nn,mm,isum,np,ja,je,ic
      
      INTEGER(2) :: obsfield(900,900)
      INTEGER(2) :: obsfieldSort(900,900)
      REAL(4) :: prgfobsfield(900,900) ! wichtig für Ausschreiben der rdc-Datei
      INTEGER iras(971,611), irasg(971,611), mask(971,611)
      REAL phi(900,900), xla(900,900)
      BYTE bit14,b1,b2
      byte buffer(1620138)
      integer*2 w_buffer(810069), wfehl, w1, w2, wert
      equivalence (buffer,w_buffer)
      INTEGER nfehl, nb14
      INTEGER*4 ivalue

      DATA iras/593281*-999/
      CHARACTER(LEN=50) cheader  !ergänzt Gra
      CHARACTER*1,YROUTFS,YFILELA*44,YFILEPHI*42,YFILERAS*43

! Fehlkennung
      DATA wfehl/B"1100010000101001"/

! RoutineFileSystem e/w erfragen
      CALL GETENV ('ROUTFS',YROUTFS)

! -------------------------------------------------------------------------------------------
 


!****************************************************************************************
!
!   Read command line arguments
!
!******************************************************************************************
    PRINT *, 'READ ARGUMENTS FROM COMMAND LINE'

!   Name for the input file
    CALL GETARG(1,chFileInput)
     if(chFileInput.eq.'') then
      print*, 'No input file given <name>.'
      bError=.TRUE.
     endif

!   input date
    CALL GETARG(2,chDate)
     if(chDate.eq.'') then
      print*, 'No date given <name>.'
      bError=.TRUE.
     endif




!*******************************************************************************
!
!   Allocate and initialize arrays
!
!*******************************************************************************

     !chYear = chDateInput(5:6)
     !chMonth = chDateInput(3:4)
     !chDay = chDateInput(1:2)
     !chHour = chDateInput(7:8)
     !chMinute = chDateInput(9:10)

     !chDate = '20'//chYear//chMonth//chDay//chHour//chMinute
     PRINT *, chDate

     chFileOutput=chDate//'_raa01_rw'
     chNameFiletitle='radolan mm/h'
     iDatatype=2 

     iidim = 900
     jidim = 900


!*****************************************************************************************
!
!	Compute rainfall retrieval for all groups in the input directory
!
!*****************************************************************************************

!-----------------------------------------------------------------------------------------
!---- Open files and read data -----------------------------------------------------------
!-----------------------------------------------------------------------------------------

      PRINT *,' Reading ', trim(chFileInput)


!---- Zuerst Header einlesen um Recordlänge zu bestimmen, Gra
!-------------------------------------------------------------
      OPEN(unit=501,file=trim(chFileInput),form='unformatted',access='direct',recl=50) 

      READ(501,rec=1) cheader
      WRITE (*,'(''READ 501 mit irm ='',i6)') irm  !Änderung Gra
      PRINT *, 'cheader: ', cheader
      CLOSE(501)
      

!      print *,cheader
            read (cheader(20:26),'(I10)') ivalue
            print *, ivalue

! Datensatz wird als File r im aktuellen Directory erwartet
!----------------------------------------------------------

!01      open(2,file='r',form='unformatted',access='direct',recl=1620138,status='old')
!      OPEN(501,file=trim(chFileInput),form='unformatted',access='direct',recl=1620146,status='old')
      OPEN(501,file=trim(chFileInput),form='unformatted',access='direct',recl=ivalue)  !temporär geändert Gra

      READ(501,rec=1,IOSTAT=irm) buffer
      !PRINT *, buffer
      WRITE (*,'(''READ 501 mit irm ='',i6)') irm  !Änderung Gra



! Stationskuerzel aus Header nach File stationen
!-----------------------------------------------

      OPEN (67,FILE='stationen')
      ja=1
      do while(buffer(ja).ne.ichar('<'))
         ja=ja+1
      enddo
!      write(*,'(i5,a1)') ja, buffer(ja)
      je=1
      do while(buffer(je).ne.ichar('>'))
         je=je+1
      enddo
!      write(*,'(i5,a1)') je, buffer(je)

      do ic=ja+1,je-1,4
        WRITE(67,'(3a1)') buffer(ic),buffer(ic+1),buffer(ic+2)
      enddo
      CLOSE (67)


! search begin/end of the header
!-------------------------------
 
      OPEN (66,FILE='header')

      ja=1
      do while(buffer(ja).ne.ichar('R'))
!         write(*,'(i5,3b9.8)') j, buffer(j)
         ja=ja+1
      enddo
!      write(*,'(i5,3b9.8)') ja, buffer(ja)

      j=1
      do while(buffer(j).ne.ichar(char(3)))
!         write(*,'(i5,3b9.8)') j, buffer(j)
         j=j+1
      enddo
!      write(*,'(i5,3b9.8)') j, buffer(j)

      WRITE (*,'(''Header-Anfang bei Byte'',i5)') ja
      WRITE (*,'(''Header-Ende   bei Byte'',i5)') j
      WRITE (*,'(146a1)') (buffer(i),i=ja,j-1) 
      WRITE (66,'(146a1)') (buffer(i),i=ja,j-1) 
!      write(*,'(i5,3b9.8)') j+1, buffer(j+1)
!      write(*,'(i5,3b9.8)') j+2, buffer(j+2)

      j=j+2
!      write(*,'(/i5,2b9.8,b20.16)') j, buffer(j-1),buffer(j),w_buffer(j/2)



! Lesen der Binaer-Daten
!-----------------------
      nb14 = 0
      nfehl = 0

      do k=1,900
         do l=1,900

! alte Fehlkennung abfragen
         IF (w_buffer(j/2) .EQ.wfehl) THEN
!!          WRITE (*,'(b9.8,b9.8,2i5,'' 1'')') buffer(j-1),buffer(j),l,k
           wert = -100
           nfehl = nfehl + 1
         ELSE

! Fehlkennung Bit 14 abfragen
           bit14 = ISHFT(buffer(j),-5)
           IF (bit14.EQ.1) THEN
!!             WRITE (*,'(2i5,b9.8,b9.8,2i5,b19.16,i10,b9.8)') k,l,buffer(j-1),buffer(j),l,k, &
!!             wert,wert,bit14
             wert = -101
             nb14 = nb14 + 1
           ELSE

!         gueltiger Wert --> Zahl steht in buffer(j-1) --> w_buffer(j/2) umsortieren
!             w1 = ISHFT(w_buffer(j/2),-8)
!             w2 = ISHFT(w_buffer(j/2),8)
!             wert = IOR(w2,w1)  ! Werte in 1/10 mm
! gueltiger Wert --> Zahl steht in buffer(j-1) --> w_buffer(j/2) umsortieren
!             w1 = ISHFT(w_buffer(j/2),-8)  ! auskommentiert Gra
!             w2 = ISHFT(w_buffer(j/2),8)   ! auskommentiert Gra
!             wert = IOR(w2,w1)  ! Werte in 1/10 mm  ! auskommentiert Gra
              wert= w_buffer(j/2)   ! eingefügt Gra

! falls wert negativ --> Bit 16 gesetzt (Clutter), wert umrechnen
             IF (wert.LT.0) THEN
                wert = wert + 32768
!!              WRITE (*,'(b9.8,b9.8,2i5,i10)') buffer(j-1),buffer(j),l,k, &
!!                wert 
             ELSE
! interpolierten Wert (Bit 13 gesetzt) umrechnen
               IF (wert.GE.4096) THEN
!!                 WRITE (*,'(2i5,b9.8,b9.8,2i5,b19.16,i10)') k,l,buffer(j-1),buffer(j),l,k, &
!!                 wert,wert
                  wert = wert - 4096   !2**12
               ENDIF                                ! Abfrage Bit 13
             ENDIF                                ! Abfrage Bit 16 (wert negativ)
           ENDIF                                ! Abfrage Bit 14 (fehlk)
         ENDIF                                ! Abfrage alte Fehlkennung
         obsfield(k,l) = wert
         j=j+2
         enddo
      enddo
      WRITE (*,'(''Anzahl Pixel wfehl  gesetzt :'',i8)') nfehl
      WRITE (*,'(''Anzahl Pixel Bit 14 gesetzt :'',i8)') nb14

200   close(2)


!---- Open files and write data -----------------------------------------------------------

       print*, 'Writing satellite datasets...'

      !PRINT *, obsfield
      OPEN (10,FILE='raster')
      DO m=1,900
        WRITE (10,'(611i4)') (irasg(m,n),n=1,900)
      ENDDO
      CLOSE (10)

     DO x=1, iidim
        DO y=1, jidim
      		
	  obsfieldSort(y,900-x+1)=obsfield(x,y)

        ENDDO
     ENDDO

       OPEN(501,FILE=Trim(chFileOutput)//'.rst',access='direct',recl=900*900*2,status='replace')
       WRITE(501,rec=1) obsfieldSort
       CLOSE(501)


       ! Write Idrisi Metadata
       prgfobsfield=real(obsfield)
       call FWWriteIdrisiMetadata &  
        ('lnx',trim(chFileOutput),trim(chNameFiletitle), &
          iDatatype,iidim,jidim, &
         'plane',0.,REAL(iidim),0.,REAL(jidim), &
         minval(prgfobsfield),maxval(prgfobsfield))


	print*,' '
	print*,'RadarBin2Rst finished.'
    end


!###############################################################################

subroutine  FWWriteIdrisiMetadata &  
     (chOS,chIdrisiFile,chMetadataTitle, &               ! Input
     iDatatype,liCols,liRows, &                         ! Input
     chRefSystem,fRefMinX,fRefMaxX,fRefMinY,fRefMaxY, & ! Input
     fDisMinValue,fDisMaxValue)                         ! Input

  !*****************************************************************************************

  !   Declaration of variables

  !*****************************************************************************************

  implicit none

  logical         ::  bErr            !! error flag

  character(len=1) :: chDirSep        !! directory separator
  character(len=*) :: chOS            !! operating system ('lnx' or 'win')
  character(len=*) ::  chRefSystem     !! Reference system
  character(len=*) ::  chMetadataTitle !! Title of the Idrisi *.rst file
  character(50)    ::  chMetadataTitlePrint !! Title of the Idrisi *.rst file
  character(len=*) ::  chIdrisiFile    !! Idrisi file name (*.rst or *.rdc)
  character(300)   ::  chIdrisiRDCFile !! Idrisi meta data file name (*.rdc)
  character(300)   ::  chIdrisiRSTFile !! Idrisi data file name (*.rst only)

  integer(2)      ::  iDatatype       !! Datatype of Idrisi file (1:byte 2:integer*2 4:real*4)
  integer(2)      ::  iLength         !! Length of a character string
  integer(2)      ::  iLengthTitle    !! Length of metadata file title
  integer(2)      ::  iIndex          !! Position of search item in a character string

  integer(4)      ::  liCols          !! Number of columns
  integer(4)      ::  liRows          !! Number of rows

  real(4)         ::  fRefMinX        !! Minimum reference system value in x direction
  real(4)         ::  fRefMaxX        !! Maximum reference system value in x direction
  real(4)         ::  fRefMinY        !! Minimum reference system value in y direction
  real(4)         ::  fRefMaxY        !! Maximum reference system value in y direction
  real(4)         ::  fMinValue       !! Minimum pixel value
  real(4)         ::  fMaxValue       !! Maximum pixel value
  real(4)         ::  fDisMinValue    !! Minimum pixel value for display
  real(4)         ::  fDisMaxValue    !! Maximum pixel value for display


  !*****************************************************************************************



  !   Format

  !*****************************************************************************************

  ! windows formats
  !   Byte metadata format
950 format('file format : IDRISI Raster A.1',/, &
       'file title  : ',a80,/, &
       'data type   : ',a4,/, &
       'file type   : binary',/, &
       'columns     : ',i8,/, &
       'rows        : ',i8,/, &
       'ref. system : ',a7,/, &
       'ref. units  : m',/, &
       'unit dist.  : 1.0000000',/, &
       'min. X      : ',f16.7,/, &
       'max. X      : ',f16.7,/, &
       'min. Y      : ',f16.7,/, &
       'max. Y      : ',f16.7,/, &
       'pos`n error : unknown',/, &
       'resolution  : unknown',/, &
       'min. value  : ',i8,/, &
       'max. value  : ',i8,/, &
       'display min : ',i8,/, &
       'display max : ',i8,/, &
       'value units : unspecified',/, &
       'value error : unknown',/, &
       'flag value  : none',/, &
       'flag def`n  : none',/, &
       'legend cats : 0')

  !   Integer*2 metadata format
951 format('file format : IDRISI Raster A.1',/, &
       'file title  : ',a80,/, &
       'data type   : ',a7,/, &
       'file type   : binary',/, &
       'columns     : ',i8,/, &
       'rows        : ',i8,/, &
       'ref. system : ',a7,/, &
       'ref. units  : m',/, &
       'unit dist.  : 1.0000000',/, &
       'min. X      : ',f16.7,/, &
       'max. X      : ',f16.7,/, &
       'min. Y      : ',f16.7,/, &
       'max. Y      : ',f16.7,/, &
       'pos`n error : unknown',/, &
       'resolution  : unknown',/, &
       'min. value  : ',i8,/, &
       'max. value  : ',i8,/, &
       'display min : ',i8,/, &
       'display max : ',i8,/, &
       'value units : unspecified',/, &
       'value error : unknown',/, &
       'flag value  : none',/, &
       'flag def`n  : none',/, &
       'legend cats : 0')

  !   Real*4 metadata format
952 format('file format : IDRISI Raster A.1',/, &
       'file title  : ',a80,/, &
       'data type   : ',a4,/, &
       'file type   : binary',/, &
       'columns     : ',i8,/, &
       'rows        : ',i8,/, &
       'ref. system : ',a7,/, &
       'ref. units  : m',/, &
       'unit dist.  : 1.0000000',/, &
       'min. X      : ',f16.7,/, &
       'max. X      : ',f16.7,/, &
       'min. Y      : ',f16.7,/, &
       'max. Y      : ',f16.7,/, &
       'pos`n error : unknown',/, &
       'resolution  : unknown',/, &
       'min. value  : ',f10.2,/, &
       'max. value  : ',f10.2,/, &
       'display min : ',f10.2,/, &
       'display max : ',f10.2,/, &
       'value units : unspecified',/, &
       'value error : unknown',/, &
       'flag value  : none',/, &
       'flag def`n  : none',/, &
       'legend cats : 0')

  ! linux formats
  !   Byte metadata format
850 format('file format : IDRISI Raster A.1',a1,/, &
       'file title  : ',a80,a1,/, &
       'data type   : ',a4,a1,/, &
       'file type   : binary',a1,/, &
       'columns     : ',i8,a1,/, &
       'rows        : ',i8,a1,/, &
       'ref. system : ',a7,a1,/, &
       'ref. units  : m',a1,/, &
       'unit dist.  : 1.0000000',a1,/, &
       'min. X      : ',f16.7,a1,/, &
       'max. X      : ',f16.7,a1,/, &
       'min. Y      : ',f16.7,a1,/, &
       'max. Y      : ',f16.7,a1,/, &
       'pos`n error : unknown',a1,/, &
       'resolution  : unknown',a1,/, &
       'min. value  : ',i8,a1,/, &
       'max. value  : ',i8,a1,/, &
       'display min : ',i8,a1,/, &
       'display max : ',i8,a1,/, &
       'value units : unspecified',a1,/, &
       'value error : unknown',a1,/, &
       'flag value  : none',a1,/, &
       'flag def`n  : none',a1,/, &
       'legend cats : 0',a1)

  !   Integer*2 metadata format
851 format('file format : IDRISI Raster A.1',a1,/, &
       'file title  : ',a80,a1,/, &
       'data type   : ',a7,a1,/, &
       'file type   : binary',a1,/, &
       'columns     : ',i8,a1,/, &
       'rows        : ',i8,a1,/, &
       'ref. system : ',a7,a1,/, &
       'ref. units  : m',a1,/, &
       'unit dist.  : 1.0000000',a1,/, &
       'min. X      : ',f16.7,a1,/, &
       'max. X      : ',f16.7,a1,/, &
       'min. Y      : ',f16.7,a1,/, &
       'max. Y      : ',f16.7,a1,/, &
       'pos`n error : unknown',a1,/, &
       'resolution  : unknown',a1,/, &
       'min. value  : ',i8,a1,/, &
       'max. value  : ',i8,a1,/, &
       'display min : ',i8,a1,/, &
       'display max : ',i8,a1,/, &
       'value units : unspecified',a1,/, &
       'value error : unknown',a1,/, &
       'flag value  : none',a1,/, &
       'flag def`n  : none',a1,/, &
       'legend cats : 0',a1)

  !   Real*4 metadata format
852 format('file format : IDRISI Raster A.1',a1,/, &
       'file title  : ',a80,a1,/, &
       'data type   : ',a4,a1,/, &
       'file type   : binary',a1,/, &
       'columns     : ',i8,a1,/, &
       'rows        : ',i8,a1,/, &
       'ref. system : ',a7,a1,/, &
       'ref. units  : m',a1,/, &
       'unit dist.  : 1.0000000',a1,/, &
       'min. X      : ',f16.7,a1,/, &
       'max. X      : ',f16.7,a1,/, &
       'min. Y      : ',f16.7,a1,/, &
       'max. Y      : ',f16.7,a1,/, &
       'pos`n error : unknown',a1,/, &
       'resolution  : unknown',a1,/, &
       'min. value  : ',f10.2,a1,/, &
       'max. value  : ',f10.2,a1,/, &
       'display min : ',f10.2,a1,/, &
       'display max : ',f10.2,a1,/, &
       'value units : unspecified',a1,/, &
       'value error : unknown',a1,/, &
       'flag value  : none',a1,/, &
       'flag def`n  : none',a1,/, &
       'legend cats : 0',a1)




  !*****************************************************************************************

  !   Write metadata to Idrisi *.rdc file

  !*****************************************************************************************

!   Set directory separator
  if(chOS.EQ.'win') then
     chDirSep = CHAR(92)
  elseif(chOS.EQ.'lnx') then
     chDirSep = CHAR(92)
  else
     bErr = .TRUE.
     return
  endif

  !   Set filename of the actual Idrisi *.rdc file (check for lower/upper letters)
  iIndex = index(chIdrisiFile,'.rdc')
  if(iIndex.eq.0) then
     iIndex = index(chIdrisiFile,'.RDC')
  endif
  if(iIndex.eq.0) then
     iIndex = index(chIdrisiFile,'.rst')
  endif
  if(iIndex.eq.0) then
     iIndex = index(chIdrisiFile,'.RST')
  endif
  if(iIndex.eq.0) then
   chIdrisiRDCFile = trim(chIdrisiFile)//'.rdc'
   chIdrisiRSTFile = trim(chIdrisiFile)//'.rst'
  else
   chIdrisiRDCFile = chIdrisiFile(1:iIndex)//'rdc'
   chIdrisiRSTFile = chIdrisiFile(1:iIndex)//'rst'
  endif 
  !   Set title if not explicitly given
  if(chMetadataTitle.eq.'') then
     iLength = len_trim(chIdrisiRDCFile)
     iIndex = index(chIdrisiRDCFile,chDirSep,BACK=.TRUE.)
     chMetadataTitlePrint = chIdrisiRDCFile(iIndex:iLength)
     iIndex = index(chMetadataTitle,'.rdc')
     chMetadataTitlePrint = chIdrisiRDCFile(1:iIndex-1)
  else
     chMetadataTitlePrint = chMetadataTitle
  endif
   iLengthTitle = len_trim(chMetadataTitle)

  !   Get extreme values and display extrema
  fMinValue = fDisMinValue
  fMaxValue = fDisMaxValue
  if(fDisMinValue.eq.0.and.fDisMaxValue.eq.0) then
     fDisMinValue = fMinValue
     fDisMaxValue = fMaxValue
  endif

  !   Write data to the Idrisi *.rdc file with respect to the data format
  open(500,file=trim(chIdrisiRDCFile))
  !   write windows
  if(chOS.EQ.'win') then
     !   Byte binary
     if(iDatatype.eq.1) then
        write(500,950) &
             chMetadataTitle, &
             'byte', &
             liCols, &
             liRows, &
             chRefSystem, &
             fRefMinX, &
             fRefMaxX, &
             fRefMinY, &
             fRefMaxY, &
             int(fMinValue), &
             int(fMaxValue), &
             int(fDisMinValue), &
             int(fDisMaxValue)

        !   Integer*2 binary
     elseif(iDatatype.eq.2) then
        write(500,951) &
             chMetadataTitle, &
             'integer', &
             liCols, &
             liRows, &
             chRefSystem, &
             fRefMinX, &
             fRefMaxX, &
             fRefMinY, &
             fRefMaxY, &
             int(fMinValue), &
             int(fMaxValue), &
             int(fDisMinValue), &
             int(fDisMaxValue)

        !   Real*4 binary
     else
        write(500,952) &
             chMetadataTitle, &
             'real   ', &
             liCols, &
             liRows, &
             chRefSystem, &
             fRefMinX, &
             fRefMaxX, &
             fRefMinY, &
             fRefMaxY, &
             fMinValue, &
             fMaxValue, &
             fDisMinValue, &
             fDisMaxValue
     end if
     ! write linux
  elseif(chOS.EQ.'lnx') then
     !   Byte binary
     if(iDatatype.eq.1) then
        write(500,850) &
             CHAR(13), &
             chMetadataTitlePrint,CHAR(13), &
             'byte',CHAR(13), &
             CHAR(13), &
             liCols,CHAR(13), &
             liRows,CHAR(13), &
             chRefSystem,CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             fRefMinX,CHAR(13), &
             fRefMaxX,CHAR(13), &
             fRefMinY,CHAR(13), &
             fRefMaxY,CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             int(fMinValue),CHAR(13), &
             int(fMaxValue),CHAR(13), &
             int(fDisMinValue),CHAR(13), &
             int(fDisMaxValue),CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             CHAR(13)

        !   Integer*2 binary
     elseif(iDatatype.eq.2) then
        write(500,851) &
             CHAR(13), &
             chMetadataTitlePrint,CHAR(13), &
             'integer',CHAR(13), &
             CHAR(13), &
             liCols,CHAR(13), &
             liRows,CHAR(13), &
             chRefSystem,CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             fRefMinX,CHAR(13), &
             fRefMaxX,CHAR(13), &
             fRefMinY,CHAR(13), &
             fRefMaxY,CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             int(fMinValue),CHAR(13), &
             int(fMaxValue),CHAR(13), &
             int(fDisMinValue),CHAR(13), &
             int(fDisMaxValue),CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             CHAR(13)

        !   Real*4 binary
     else
        write(500,852) &
             CHAR(13), &
             chMetadataTitlePrint,CHAR(13), &
             'real   ',CHAR(13), &
             CHAR(13), &
             liCols,CHAR(13), &
             liRows,CHAR(13), &
             chRefSystem,CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             fRefMinX,CHAR(13), &
             fRefMaxX,CHAR(13), &
             fRefMinY,CHAR(13), &
             fRefMaxY,CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             fMinValue,CHAR(13), &
             fMaxValue,CHAR(13), &
             fDisMinValue,CHAR(13), &
             fDisMaxValue,CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             CHAR(13), &
             CHAR(13)
     end if
  endif

  close(500)

  return

end subroutine FWWriteIdrisiMetadata



